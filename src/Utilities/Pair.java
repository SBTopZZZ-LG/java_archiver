package Utilities;

import Models.Signature;

import java.util.Objects;

public class Pair<T, K> implements Signature {
    public T first;
    public K second;

    public Pair(T first, K second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Signature signatureObj = (Signature) obj;
            return signatureObj.getSignature() == getSignature();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getSignature() {
        return Objects.hash(first, second);
    }
}
