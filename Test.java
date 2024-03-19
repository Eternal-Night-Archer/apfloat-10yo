


// 1. object instantiation statements
float real_value = 1.0f;
float imag_value = 2.0f;
Apfloat real = new Apfloat(real_value);
Apfloat imag = new Apfloat(imag_value);
Apcomplex c1 = new Apcomplex(real, imag);


// 2. import statements
import org.apfloat.Apcomplex;
import org.apfloat.Apfloat;



// FastTimeZone.java
public static TimeZone
getGmtTimeZone(final String pattern) {
  if ("Z".equals(pattern) || "UTC".equals(pattern)) {
    return GREENWICH;
  }
  final Matcher m = GMT_PATTERN.matcher(pattern);
  if (m.matches()) {
    final int hours = parseInt(m.group(2));
    final int minutes = parseInt(m.group(4));
    if (hours == 0 && minutes == 0) {
      return GREENWICH;
    }
    return new GmtTimeZone(
      parseSign(m.group(1)), hours, minutes);
  }
  return null;
}

// Apcomplex.java
public class Apcomplex extends Number {
  private Apfloat real;
  private Apfloat imag;
  public Apcomplex(Apfloat real, Apfloat imag) ..
  public Apcomplex(String value) ..
  // overlook other constructors
}

// Apfloat.java
public class Apfloat extends Apcomplex {
  private ApfloatImpl impl;
  public Apfloat(long value) ..
  public Apfloat(String value, long precision) ..
  // overlook other constructors
}


