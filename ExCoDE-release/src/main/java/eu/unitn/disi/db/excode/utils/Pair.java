package eu.unitn.disi.db.excode.utils;

import java.util.Objects;

public class Pair<A, B> {

    private A a;
    private B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.a);
        hash = 97 * hash + Objects.hashCode(this.b);
        return hash;
    }

    public boolean equals(Object aThat) {
        if (this == aThat) {
            return true;
        }
        return (this.a.equals(((Pair) aThat).getA()) && this.b.equals(((Pair) aThat).getB()));
    }
    
    public int compareTo(Pair p) {
        if (this.a instanceof Integer) {
            return Integer.compare((Integer) this.a, (Integer) p.a);
        } else if (this.a instanceof Double) {
            return Double.compare((Double) this.a, (Double) p.a);
        }
        throw new UnsupportedOperationException("Compare not supported for this type of structure");
    }
}