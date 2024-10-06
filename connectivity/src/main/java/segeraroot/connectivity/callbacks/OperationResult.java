package segeraroot.connectivity.callbacks;

public enum OperationResult {
    CONTINUE, DONE;

    public static OperationResult isDone(boolean isDone) {
        return isDone ? DONE : CONTINUE;
    }
}
