package com.dci.intellij.dbn.data.model.sortable;

import com.dci.intellij.dbn.data.model.DataModelRow;
import com.dci.intellij.dbn.data.model.DataModelState;
import com.dci.intellij.dbn.data.model.basic.BasicDataModel;
import com.dci.intellij.dbn.data.sorting.SortDirection;
import com.dci.intellij.dbn.data.sorting.SortingState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class SortableDataModel<T extends SortableDataModelRow> extends BasicDataModel<T> {
    protected SortableDataModel(Project project) {
        super(project);
    }

    public boolean sort(int columnIndex, SortDirection direction, boolean keepExisting) {
        boolean sort = updateSortingState(columnIndex, direction, keepExisting);
        if (sort) {
            sort();
            notifyRowsUpdated(0, getRows().size());
        }
        return sort;
    }

    @NotNull
    @Override
    public SortableDataModelState getState() {
        return (SortableDataModelState) super.getState();
    }

    @Override
    protected DataModelState createState() {
        return new SortableDataModelState();
    }

    protected boolean updateSortingState(int columnIndex, SortDirection direction, boolean keepExisting) {
        SortingState sortingState = getSortingState();
        String columnName = getColumnName(columnIndex);
        return sortingState.applySorting(columnName, direction, keepExisting);
    }

    protected void sortByIndex() {
        Collections.sort(getRows(), INDEX_COMPARATOR);
        updateRowIndexes(0);
    }

    public void sort() {
        sort(getRows());
    }

    protected void sort(List<T> rows) {
        if (getSortingState().isValid()) {
            Collections.sort(rows);
        }
        updateRowIndexes(rows, 0);
    }

    private static final Comparator<DataModelRow> INDEX_COMPARATOR = new Comparator<DataModelRow>() {
        public int compare(DataModelRow row1, DataModelRow row2) {
            return row1.getIndex() - row2.getIndex();
        }
    };


/*
    public SortDirection getSortDirection() {
        return getSortingState().getDirection();
    }
*/

    public SortingState getSortingState() {
        return getState().getSortingState();
    }

/*
    public int getSortColumnIndex() {
        // fixme - cache sort column index somehow
        SortableDataModelState modelState = getState();
        return isDisposed() || modelState == null ? -1 :
                getHeader().getColumnIndex(modelState.getSortingState().getColumnName());
    }
*/
}
