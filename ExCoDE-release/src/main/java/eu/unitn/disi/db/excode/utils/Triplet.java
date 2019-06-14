package eu.unitn.disi.db.excode.utils;

import java.util.Objects;

/**
 *
 * @author bluecopper
 */
public class Triplet<A, B, C> {

    private A a;
    private B b;
    private C c;

    public Triplet(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }
    
    public C getC() {
        return c;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.a);
        hash = 29 * hash + Objects.hashCode(this.b);
        hash = 29 * hash + Objects.hashCode(this.c);
        return hash;
    }

    public boolean equals(Object aThat) {
        if (this == aThat) {
            return true;
        }
        return (this.a.equals(((Triplet) aThat).getA()) && this.b.equals(((Triplet) aThat).getB()) && this.c.equals(((Triplet) aThat).getC()));
    }
}
