package com.dci.intellij.dbn.language.common.element.parser.impl;

import com.dci.intellij.dbn.language.common.ParseException;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.element.BlockElementType;
import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.IdentifierElementType;
import com.dci.intellij.dbn.language.common.element.IterationElementType;
import com.dci.intellij.dbn.language.common.element.SequenceElementType;
import com.dci.intellij.dbn.language.common.element.parser.AbstractElementTypeParser;
import com.dci.intellij.dbn.language.common.element.parser.ParseResult;
import com.dci.intellij.dbn.language.common.element.parser.ParseResultType;
import com.dci.intellij.dbn.language.common.element.parser.ParserBuilder;
import com.dci.intellij.dbn.language.common.element.parser.ParserContext;
import com.dci.intellij.dbn.language.common.element.path.ParsePathNode;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.element.util.ParseBuilderErrorHandler;
import com.intellij.lang.PsiBuilder;
import org.jetbrains.annotations.NotNull;

public class SequenceElementTypeParser<ET extends SequenceElementType> extends AbstractElementTypeParser<ET> {
    public SequenceElementTypeParser(ET elementType) {
        super(elementType);
    }

    public ParseResult parse(@NotNull ParsePathNode parentNode, boolean optional, int depth, ParserContext context) throws ParseException {
        ParserBuilder builder = context.getBuilder();
        logBegin(builder, optional, depth);
        SequenceElementType elementType = getElementType();

        ParsePathNode node = createParseNode(parentNode, builder.getCurrentOffset());
        PsiBuilder.Marker marker = builder.mark(node);
        int matches = 0;
        int matchedTokens = 0;

        TokenType tokenType = builder.getTokenType();
        boolean isDummyToken = isDummyToken(builder.getTokenText());
        boolean isSuppressibleReservedWord =
                !elementType.is(ElementTypeAttribute.STATEMENT) &&
                isSuppressibleReservedWord(tokenType, node);


        if (tokenType != null && !tokenType.isChameleon() && (isDummyToken || isSuppressibleReservedWord || elementType.getLookupCache().canStartWithToken(tokenType))) {
            ElementType[] elementTypes = elementType.getElementTypes();
            while (node.getCurrentSiblingIndex() < elementTypes.length) {
                int index = node.getCurrentSiblingIndex();
                // is end of document
                if (tokenType == null || tokenType.isChameleon()) {
                    ParseResultType resultType =
                            elementType.isOptional(index) && (elementType.isLast(index) || elementType.isOptionalFromIndex(index)) ? ParseResultType.FULL_MATCH :
                            !elementType.isFirst(index) && !elementType.isOptionalFromIndex(index) && !elementType.isExitIndex(index) ? ParseResultType.PARTIAL_MATCH : ParseResultType.NO_MATCH;
                    return stepOut(marker, depth, resultType, matchedTokens, node, context);
                }

                ParseResult result = ParseResult.createNoMatchResult();
                // current token can still be part of the iterated element.
                //if (elementTypes[i].containsToken(tokenType)) {
                if (isDummyToken || elementTypes[index].getLookupCache().canStartWithToken(tokenType) || isSuppressibleReservedWord(tokenType, node)) {

                    //node = node.createVariant(builder.getCurrentOffset(), i);
                    result = elementTypes[index].getParser().parse(node, elementType.isOptional(index), depth + 1, context);

                    if (result.isMatch()) {
                        matchedTokens = matchedTokens + result.getMatchedTokens();
                        tokenType = builder.getTokenType();
                        isDummyToken = isDummyToken(builder.getTokenText());
                        matches++;
                    }
                }

                // not matched and not optional
                if (result.isNoMatch() && !elementType.isOptional(index)) {
                    boolean isWeakMatch = matches < 2 && matchedTokens < 3 && index > 1 && ignoreFirstMatch();
                    
                    if (elementType.isFirst(index) || elementType.isExitIndex(index) || isWeakMatch || matches == 0) {
                        //if (isFirst(i) || isExitIndex(i)) {
                        return stepOut(marker, depth, ParseResultType.NO_MATCH, matchedTokens, node, context);
                    }

                    index = advanceLexerToNextLandmark(node, context);

                    if (index <= 0) {
                        // no landmarks found or landmark in parent found
                        return stepOut(marker, depth, ParseResultType.PARTIAL_MATCH, matchedTokens, node, context);
                    } else {
                        // local landmarks found

                        tokenType = builder.getTokenType();
                        isDummyToken = isDummyToken(builder.getTokenText());

                        node.setCurrentSiblingIndex(index);
                        continue;
                    }
                }

                // if is last element
                if (elementType.isLast(index)) {
                    //matches == 0 reaches this stage only if all sequence elements are optional
                    ParseResultType resultType = matches == 0 ? ParseResultType.NO_MATCH : ParseResultType.FULL_MATCH;
                    return stepOut(marker, depth, resultType, matchedTokens, node, context);
                }
                node.incrementIndex(builder.getCurrentOffset());
            }
        }

        return stepOut(marker, depth, ParseResultType.NO_MATCH, matchedTokens, node, context);
    }

    private boolean ignoreFirstMatch() {
        ElementType firstElementType = getElementType().getElementTypes()[0];
        if (firstElementType instanceof IdentifierElementType) {
            IdentifierElementType identifierElementType = (IdentifierElementType) firstElementType;
            return !identifierElementType.isDefinition();
        }
        return false;
    }

    @Override
    protected ParseResult stepOut(PsiBuilder.Marker marker, int depth, ParseResultType resultType, int matchedTokens, ParsePathNode node, ParserContext context) {
        ParserBuilder builder = context.getBuilder();
        if (resultType == ParseResultType.NO_MATCH) {
            builder.markerRollbackTo(marker, node);
        } else {
            if (getElementType() instanceof BlockElementType)
                builder.markerDrop(marker); else
                builder.markerDone(marker, getElementType(), node);
        }

        return super.stepOut(null, depth, resultType, matchedTokens, node, context);
    }    

    private int advanceLexerToNextLandmark(ParsePathNode node, ParserContext context) throws ParseException {
        int siblingPosition = node.getCurrentSiblingIndex();
        ParserBuilder builder = context.getBuilder();
        PsiBuilder.Marker marker = builder.mark(null);
        SequenceElementType elementType = getElementType();
        ParseBuilderErrorHandler.updateBuilderError(elementType.getFirstPossibleTokensFromIndex(siblingPosition), context);

        TokenType tokenType = builder.getTokenType();
        siblingPosition++;
        while (tokenType != null) {
            int newIndex = getLandmarkIndex(tokenType, siblingPosition, node);

            // no landmark hit -> spool the builder
            if (newIndex == 0) {
                builder.advanceLexer(node);
                tokenType = builder.getTokenType();
            } else {
                //builder.markerDone(marker, getElementBundle().getUnknownElementType());
                marker.error("Unrecognized statement");
                return newIndex;
            }
        }
        //builder.markerDone(marker, getElementBundle().getUnknownElementType());
        marker.error("Unrecognized statement 1");
        return 0;
    }

    protected int getLandmarkIndex(TokenType tokenType, int index, ParsePathNode node) {
        if (tokenType.isParserLandmark()) {
            ElementType[] elementTypes = getElementType().getElementTypes();
            for (int i=index; i< elementTypes.length; i++) {
                // check children landmarks
                if (elementTypes[i].getLookupCache().canStartWithToken(tokenType)) {
                    return i;
                }
            }

            ParsePathNode parseNode = node;
            while (parseNode != null) {
                ElementType elementType = parseNode.getElementType();
                if (elementType instanceof SequenceElementType) {
                    SequenceElementType sequenceElementType = (SequenceElementType) elementType;
                    if ( sequenceElementType.containsLandmarkTokenFromIndex(tokenType, parseNode.getCurrentSiblingIndex() + 1)) {
                        return -1;
                    }
                } else  if (elementType instanceof IterationElementType) {
                    IterationElementType iterationElementType = (IterationElementType) elementType;
                    if (iterationElementType.isSeparator(tokenType)) {
                        return -1;
                    }
                }
                parseNode = parseNode.getParent();
            }
        }
        return 0;
    }


    protected ParsePathNode advanceToLandmark_New(ParserBuilder builder, ParsePathNode node) {
        TokenType tokenType = builder.getTokenType();
        while (tokenType != null && !tokenType.isParserLandmark()) {
            builder.advanceLexer(node);
            tokenType = builder.getTokenType();
        }

        // scan current sequence
        ElementType[] elementTypes = getElementType().getElementTypes();
        int siblingIndex = node.getCurrentSiblingIndex();
        while (siblingIndex < elementTypes.length) {
            int builderOffset = builder.getCurrentOffset();
            siblingIndex = node.incrementIndex(builderOffset);
            // check children landmarks
            if (elementTypes[siblingIndex].getLookupCache().canStartWithToken(tokenType)) {
                return node;
            }
        }

        ParsePathNode parentNode = node.getParent();
        while (parentNode != null) {
            ElementType elementType = parentNode.getElementType();
            if (elementType instanceof SequenceElementType) {
                parentNode = advanceToLandmark_New(builder, node);

            } else if (elementType instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) elementType;
                if (iterationElementType.isSeparator(tokenType)) {
                    return parentNode;
                }
            }
            parentNode = parentNode.getParent();
        }

        return null;
    }


}
