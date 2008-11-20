test 1
caught java.lang.ArithmeticException: / by zero
java.lang.ArithmeticException: / by zero
	at Test.testHardwareException(Test.java:30)
	at Test.trouble(Test.java:60)
	at Test.main(Test.java:73)
test 2
caught java.lang.NumberFormatException: For input string: "abc"
java.lang.NumberFormatException: For input string: "abc"
	at sun.misc.FloatingDecimal.readJavaFormatString(FloatingDecimal.java:1224)
	at java.lang.Float.valueOf(Float.java:360)
	at Test.testSoftwareException(Test.java:38)
	at Test.trouble(Test.java:61)
	at Test.main(Test.java:73)
test 3
caught java.io.IOException
java.io.IOException
	at Test.testUserException(Test.java:46)
	at Test.trouble(Test.java:62)
	at Test.main(Test.java:73)
test 4
caught java.lang.NumberFormatException: For input string: "abc"
java.lang.NumberFormatException: For input string: "abc"
	at sun.misc.FloatingDecimal.readJavaFormatString(FloatingDecimal.java:1224)
	at java.lang.Float.valueOf(Float.java:360)
	at Test.testSoftwareException(Test.java:38)
	at Test.<init>(Test.java:18)
	at Test.testRethrownException(Test.java:53)
	at Test.trouble(Test.java:63)
	at Test.main(Test.java:73)
