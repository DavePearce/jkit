package whiley.util;

import java.math.BigInteger;

public final class BigRational extends Number implements Comparable<BigRational> {	
	private static final BigRational[] cache = new BigRational[20];

	public static final BigRational MONE = BigRational.valueOf(-1);
	public static final BigRational ZERO = new BigRational(BigInteger.ZERO);
	public static final BigRational ONE = new BigRational(BigInteger.ONE);
	
	private final BigInteger numerator;
	private final BigInteger denominator;

	public BigRational(String val) {		
		int idx = val.indexOf('.');
		if(idx > 0) {
			String lhs = val.substring(0,idx);
			String rhs = val.substring(idx+1);
			BigInteger num = new BigInteger(lhs + rhs);			
			BigInteger den = BigInteger.valueOf(10).pow(rhs.length());			
			BigInteger gcd = num.gcd(den);			
			if(!gcd.equals(BigInteger.ONE)) {
				num = num.divide(gcd);
				den = den.divide(gcd);
			}
			
			// normalise sign.
			BigInteger zero = BigInteger.ZERO;
			if(zero.compareTo(num) > 0 && zero.compareTo(den) >= 0) {
				num = num.negate();
				den = den.negate();
			} else if(zero.compareTo(den) == 0) {
				num = BigInteger.ZERO;
				den = BigInteger.ZERO;
			}
			
			// done, assign final fields.
			numerator = num;
			denominator = den;			
		} else {
			numerator = new BigInteger(val);
			denominator = BigInteger.valueOf(1);
		}		
	}
	
	public BigRational(BigInteger numerator) {
		this.numerator = numerator;
		this.denominator = BigInteger.ONE;
	}
	
	public BigRational(BigInteger numerator, BigInteger denominator) {			
		BigInteger gcd = numerator.gcd(denominator);		
		if(!gcd.equals(BigInteger.ONE)) {
			numerator = numerator.divide(gcd);
			denominator = denominator.divide(gcd);
		}
		
		// normalise sign.
		BigInteger zero = BigInteger.ZERO;
		if(zero.compareTo(numerator) > 0 && zero.compareTo(denominator) > 0) {
			numerator = numerator.negate();
			denominator = denominator.negate();
		} else if(zero.compareTo(denominator) == 0) {
			numerator = BigInteger.ZERO;
			denominator = BigInteger.ZERO;
		}
		
		// done, assign final fields.
		this.numerator = numerator;
		this.denominator = denominator;
	}

	public BigRational(int numerator, int denominator) {
		this(BigInteger.valueOf(numerator),BigInteger.valueOf(denominator));
	}
	
	public BigInteger numerator() {
		return numerator;
	}
	
	public BigInteger denominator() {
		return denominator;
	}
	
	/**
	 * Get an integer representation of this rational. <b>It is strongly
	 * recommended against using this method, since in most cases it will be
	 * very imprecise.</b>
	 */
	public int intValue() {
		long l = numerator.longValue();
		return (int) (l / denominator.longValue());
	}
	
	/**
	 * Get a long representation of this rational. <b>It is strongly
	 * recommended against using this method, since in most cases it will be
	 * very imprecise.</b>
	 */
	public long longValue() {
		long l = numerator.longValue();
		return l / denominator.longValue();
	}
	
	public float floatValue() {
		float l = numerator.floatValue();
		return l / denominator.floatValue();
	}
	
	public double doubleValue() {
		double l = numerator.doubleValue();
		return l / denominator.doubleValue();
	}
	
	public boolean equals(Object o) {
		if (o instanceof BigRational) {
			BigRational r = (BigRational) o;
			return numerator.equals(r.numerator)
					&& denominator.equals(r.denominator);
		}
		return false;
	}
	
	public int hashCode() {
		return numerator.hashCode() + denominator.hashCode();
	}
	
	public int compareTo(BigRational r) {
		BigInteger lhs = numerator.multiply(r.denominator);
		BigInteger rhs = r.numerator.multiply(denominator);
		return lhs.compareTo(rhs);
	}
	
	public String toString() {
		return numerator.toString() + "/" + denominator.toString();		
	}
	
	// =========================================================
	// ==================== ADDITION ===========================
	// =========================================================
	
	public BigRational add(int r) {
		BigInteger num = numerator.add(denominator.multiply(BigInteger.valueOf(r)));
		return new BigRational(num,denominator);
	}
	
	public BigRational add(long r) {
		BigInteger num = numerator.add(denominator.multiply(BigInteger.valueOf(r)));
		return new BigRational(num,denominator);
	}
	
	public BigRational add(final BigInteger r) {
		BigInteger num = numerator.add(denominator.multiply(r));
		return new BigRational(num,denominator);
	}
	
	public BigRational add(final BigRational r) {
		BigInteger num = numerator.multiply(r.denominator).add(r.numerator.multiply(denominator));
		BigInteger den = denominator.multiply(r.denominator);
		return new BigRational(num,den);
	}
	
	// =========================================================
	// ==================== SUBTRACTION ========================
	// =========================================================
	
	public BigRational subtract(int r) {
		BigInteger num = numerator.subtract(denominator.multiply(BigInteger.valueOf(r)));
		return new BigRational(num,denominator);
	}
	
	public BigRational subtract(long r) {
		BigInteger num = numerator.subtract(denominator.multiply(BigInteger.valueOf(r)));
		return new BigRational(num,denominator);
	}
	
	public BigRational subtract(final BigInteger r) {
		BigInteger num = numerator.subtract(denominator.multiply(r));
		return new BigRational(num,denominator);
	}
	
	public BigRational subtract(final BigRational r) {
		BigInteger num = numerator.multiply(r.denominator).subtract(
				r.numerator.multiply(denominator));
		BigInteger den = denominator.multiply(r.denominator);
		return new BigRational(num, den);
	}
	
	// =========================================================
	// ==================== MULTIPLICATION =====================
	// =========================================================
	
	public BigRational multiply(int r) {
		BigInteger num = numerator.multiply(BigInteger.valueOf(r));
		return new BigRational(num,denominator);
	}
	
	public BigRational multiply(long r) {
		BigInteger num = numerator.multiply(BigInteger.valueOf(r));
		return new BigRational(num,denominator);
	}
	
	public BigRational multiply(final BigInteger r) {
		BigInteger num = numerator.multiply(r);
		return new BigRational(num,denominator);
	}
			
	public BigRational multiply(final BigRational r) {
		BigInteger num = numerator.multiply(r.numerator);
		BigInteger den = denominator.multiply(r.denominator);
		return new BigRational(num, den);
	}
	
	// =========================================================
	// ======================= DIVISION ========================
	// =========================================================
	
	public BigRational divide(int r) {
		BigInteger den = denominator.multiply(BigInteger.valueOf(r));
		return new BigRational(numerator,den);
	}
	
	public BigRational divide(long r) {
		BigInteger den = denominator.multiply(BigInteger.valueOf(r));
		return new BigRational(numerator,den);
	}
	
	public BigRational divide(BigInteger r) {
		BigInteger den = denominator.multiply(r);
		return new BigRational(numerator,den);
	}
	
	public BigRational divide(final BigRational r) {
		BigInteger num = numerator.multiply(r.denominator);
		BigInteger den = denominator.multiply(r.numerator);
		return new BigRational(num, den);
	}
	
	// =========================================================
	// ========================= OTHER =========================
	// =========================================================
	public BigRational negate() {
		return new BigRational(numerator.negate(),denominator);
	}
	
	public static BigRational valueOf(long x) {
		if(x > -10 && x <= 10) {
			int idx = (int) x + 9;
			BigRational r = cache[idx];
			if(r == null) {
				r = new BigRational(BigInteger.valueOf(x));
				cache[idx] = r;
			}
			return r;
		} else {
			return new BigRational(BigInteger.valueOf(x));
		}
	}
	
	public static void main(String[] args) {
		BigRational r = new BigRational(10,5);
		System.out.println("GOT: " + r);
		System.out.println("NOW: " + r.negate());
	}
}
