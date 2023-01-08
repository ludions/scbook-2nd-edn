/*
MITHUnicode extensions

+ Integer
+ SequenceableCollection
+ String

© 2023 Tom Hall
ludions.com
*/

+ Integer {

	asCodepoint {|prefix = true|
		var hex = this.asHexString;
		while{hex[0]== $0 and: {hex.size>4}}{hex = hex.drop(1)};
		if(prefix, {^("U+"++hex)}, {^hex});
	}

	isStandardAscii {
		^(this.ascii>=0 and: {this<128})
	}

	isSMuFL {
		^(this>=0xE000 and: {this<=0xF8FF})
	}

	isBasicMultilingualPlane {
		^(this>=0x0000 and: {this<=0xFFFF})
	}

	// https://www.unicode.org/reports/tr29/#GB_After_Joiner
	// REGIONAL INDICATOR SYMBOL
	isRegionalIndicator {
		^(this.ascii>=127462 and: {this<127487})
	}
}


+ SequenceableCollection {

	// also equivalent to 'asAscii' in Strang class
	// and for user-perceived single glyphs that utilize multiple code points
	asGlyph {
		//try{this.asUnicodeString}{^this};
		^this.asUnicodeString
	}

	// to string array of strings including Unicode code points
	asUnicodeString {
		var string, strStack = "";
		string = this.collect{|i|
			// bool is for verbose kludge
			if(i.prStripUnicodePrefix.isUnicodeHex(false)){
				i.asGlyph
			}{
				i
			};
		};
		string.do{|i| strStack = strStack++i};
		^strStack;
	}

	// https://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&item_id=IWS-AppendixA
	// UTF-8 byte sequence Array
	asCodePoint {
		var arrSize, arr, hex, width, hexStr;
		arr = this.copy.mod(256); // convert to unsigned
		arrSize = arr.size;
		hex = case {arrSize == 1} {
			arr.unbubble
		}
		{arrSize == 2} {
			((arr[0] - 192) * 64) + (arr[1] - 128)
		}
		{arrSize == 3} {
			((arr[0] - 224) * 4096)
			+ ((arr[1] - 128) * 64)
			+ arr[2] - 128
		}
		{arrSize == 4} {
			((arr[0] - 240) * 262144)
			+ ((arr[1] - 128) * 4096)
			+ ((arr[2] - 128) * 64)
			+ arr[3] - 128
		}
		{arrSize > 4} {
			^"Array contains more than one code point".error;
		};
		width = if(arrSize<4, {4}, {6});
		hexStr = hex.asHexString(width);
		if(width==6 and:{hexStr[0]==$0}, {
			hexStr = hexStr.drop(1);
		});
		^"U+"++hexStr
	}
}


+ String {

	// used to find Unicode names nested in strings - not for glyphs
	parseCodePoints {
		var regExpr, pos, codePointRanges;
		var codePoints, matches, stack;
		var string = this.copy;

		// regular expression
		// NB  {4,5} is {min, max} so can include
		// higher codePoints incl emoji.
		// For Basic Multilingual Plane / SMuFL only
		// {4} is correct
		regExpr = "[Uu]\\+[a-fA-F0-9]{4,5}";

		// check if any codePoints
		if(regExpr.matchRegexp(string)) {

			// find position of all
			matches = string.findAllRegexp(regExpr);

			// search for len of unicode name given start from pos above
			codePointRanges = matches.collect{|i|
				pos = [i]++string.findRegexpAt(regExpr, offset: i);
				[pos[0], pos[0] + pos[2]-1]
			};

			stack = [];
			if(codePointRanges.size==1){
				var a = codePointRanges[0];
				if(a[0]>0) {stack = stack.add([0, a[0]-1])};
				stack = stack.add(a);
				stack = stack.add([a[1]+1, (string.size-1)]);
			}{
				codePointRanges.doAdjacentPairs({ arg a, b, j;
					// if first element
					if(j==0 and: {a[0]>0}) {stack = stack.add([0, a[0]-1])};
					stack = stack.add(a);
					// add non codePoint text range
					if(b[0]>(a[1]+1)) {stack = stack.add([a[1]+1, b[0]-1]) };
					// if last pair
					if(j== (codePointRanges.size-2)) {
						stack = stack.add(b);
						// check for final chars: string is original text
						if(b[1]<(string.size-1)) {stack = stack.add([b[1]+1, (string.size-1)])};
					};
				});
			};
			codePoints = stack.collect{|i| string.copyRange(*i)};
		}{
			codePoints = [string];
		};
		^codePoints
	}

	prStripUnicodePrefix {
		var string = this.copy;
		if(string.size>5 and:{string.toUpper.beginsWith("U+")}){
			^string.drop(2)
		}{
			^this
		}
	}

	isUnicodeHex { |verbose=true|
		var regExprMatch;
		regExprMatch = this.findRegexpAt("[a-fA-F0-9]{4,5}");
		if(regExprMatch.isNil) {
			if(verbose){
				format("'%' is not within valid single Unicode codespace", this).warn;
			};
			^false;
		};
		if(regExprMatch[1] != this.size){
			if(verbose){
				if("[^a-fA-F0-9]+".matchRegexp(this)){
					format("Non Unicode chars detected within string '%'", this).warn;
				}{
					format("'%' hex is outside of Unicode Supplementary Private Use Area-A codespace", this).warn;
				};
			};
			^false;
		};
		^true
	}

	// assumes this is unicode codePoint, not glyph
	asGlyph {
		^try{MITHUnicode.new(this).glyph}{this}
	}

	// catch error elsewhere?
	prAsGlyph { ^MITHUnicode.new(this).glyph }

	// assumes this is unicode codePoint, not glyph
	isSMuFL {
		^try{MITHUnicode.new(this).integer.isSMuFL}{this}
	}

	// assumes this is unicode codePoint, not glyph
	isStandardAscii {
		^try{MITHUnicode.new(this).integer.isStandardAscii}{this}
	}

	// assumes glyph
	asCodePoint {
		var arr = try{this.asCodePointArray}{^this};
		// check for single user-perceived character grapheme clusters
		if(arr.size==1){
		^try{this.ascii.asCodePoint}{this}
		}{
			"(Multiple code points found.)".inform;
		^arr
		}
	}

	// assumes glyph
	asCodePointInteger {
		^try{MITHUnicode.new(this.asCodePoint).integer}{this}
	}

	asCodePointArray {
		var str, char, ascii, counter;
		var resArr, tmpArr, asciiStr, midAscii;
		midAscii = false;
		asciiStr = "";
		tmpArr= [];
		resArr = [];
		str = this.copy;

		// adapted from Julian Rohrhuber's Strang Quark
		// asStrang method
		str.size.do { |i, j|
			char= str[i];
			ascii = char.ascii;
			if (ascii < 0, {
				// non-ascii sequence
				if(midAscii, {
					resArr = resArr.add(asciiStr);
					asciiStr = "";
					midAscii = false
				});
				case
				{ (ascii >= -64) && (ascii < -32) } { counter = 2 }
				{ (ascii >= -32) && (ascii < -16) } { counter = 3 }
				{ ascii >= -16 } { counter = 4 };
				counter = counter - 1;
				tmpArr = tmpArr.add(ascii);
				if (counter == 0, {
					resArr = resArr.add(tmpArr.asCodePoint);
					tmpArr = [];
				});
			}, {
				// ascii sequence
				asciiStr = asciiStr ++char;
				midAscii = true;
			});
		};
		if(midAscii) {
			resArr = resArr.add(asciiStr)
		};
		^resArr
	}
}


