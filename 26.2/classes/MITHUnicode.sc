/*
MITHUnicode
© 2022 Tom Hall
ludions.com
*/



MITHUnicode {
	var <numBytes, <codePoint, int, <bytes, <hex;

	*new { |codePoint|
		^super.new.init(codePoint);
	}

	// http://www.unicode.org/reports/tr29/
	*solidarity {
		var codePointArr = [["U+267B", "U+FE0F"], ["U+1F1FA", "U+1F1E6"],["U+1F6B4"],
			[ "U+1F9D1", "U+1F3FE", "U+200D", "U+2695", "U+FE0F" ],["U+1F6B6"],
			["U+1F331"], ["U+1F428"], ["U+1F3B5"], ["U+1F9D3"], ["U+2640", "U+FE0F"]
		];
		^codePointArr.scramble.collect{|i| i.asUnicodeString}.asUnicodeString
	}

	init { |aCodePoint|
		var unicodePrefix;
		codePoint = aCodePoint.asString.toUpper; // allow symbols sans prefix
		if(codePoint.beginsWith("U+")){
			hex = codePoint.prStripUnicodePrefix;
		}{
			hex = codePoint;
			codePoint = "U+"++codePoint
		};
		if(hex.isUnicodeHex.not){
			^aCodePoint
		}{
			int = ("16r" ++ hex).interpret; // hex to unsigned integer
			numBytes = this.findNumBytes;
			bytes = this.byteSequence;
		};
		^this
	}

	// ie UTF-8 Bytes
	// https://en.wikipedia.org/wiki/UTF-8#Octal
	findNumBytes {
		var someBytes, octal;
		octal= int.asStringToBase(8, 6).interpret;
		someBytes = case
		{ octal < 200 } {1}
		{ octal < 4000 } {2}
		{ octal < 200000 } {3}
		{ octal >= 200000 } {4};
		^someBytes
	}

	integer {^int}

	isAscii {
		^(numBytes == 1)
	}

	asJSONStr {
		if(int.isBasicMultilingualPlane, {
			^("\\u"++codePoint.drop(2))
		}, {
			^"Unicode is not within the required Basic Multilingual Plane".error;
		});
	}

	isStandardAscii {^this.isAscii}

	isSMuFL {^this.integer.isSMuFL}

	isRegionalIndicator  {^this.integer.isRegionalIndicator}

	// variable size UTF-8 byte sequence Array
	byteSequence {
		var byteArr;
		byteArr = case {numBytes ==1 } {[int]}
		{numBytes ==2 } {[int/64 +192, int.mod(64)+128]}
		{numBytes ==3 }{
			[int/4096 + 224, (int.mod(4096) / 64) + 128, int.mod(64) + 128]
		}
		{numBytes ==4 }{
			[
				int/262144 + 240,
				int.mod(262144) / 4096 +128,
				(int.mod(4096) / 64) + 128, int.mod(64) + 128
			]
		};
		^byteArr.asInteger; // convert from floats
	}

	bytesSigned {^bytes.wrap(-127, 127) -1 }

	glyph {
		^bytes.collect{|i|
			i.asAscii; // see Integer method: "// must be 0 <= this <= 255"
		}.as(String)
	}

}
