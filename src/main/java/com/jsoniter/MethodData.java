package com.jsoniter;

import java.util.Arrays;

public class MethodData {
    int numberOfBranches;
    public boolean[] branchReached;

    public MethodData(int numberOfBranches) {
        this.numberOfBranches = numberOfBranches;
        this.branchReached = new boolean[numberOfBranches];
    }

    @Override
    public String toString() {
        return Arrays.toString(branchReached);
    }

    public double getCoverage() {
        double count = 0;
        for (boolean bool : this.branchReached) {
            count += (bool ? 1 : 0);
        }
        return count / numberOfBranches;
    }
}