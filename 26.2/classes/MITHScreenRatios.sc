
/*
MITHScreenRatios
MITHScreenRatiosView

Dependency: RatioArray

© 2022 - 2025 Tom Hall
ludions.com

*/

MITHScreenRatios {
	var  <>screenDims, <viewDims, <ratiosDict, <maxDims;

	*ratiosDict {
		^Dictionary.with(*[
			"square" -> [[1, 1], "unison"],
			"r16_9" -> [[16, 9], "minor 7th"],
			"r4_3" -> [[4, 3], "perfect 4th"],
			"r3_2" -> [[3, 2], "perfect 5th"],
			"iso" -> [[7071, 5000], "tritone / sqrt(2)"],
			"phi" -> [[809, 500], "(φ: (1 + sqrt(5))/2"],
			"usLetter" -> [[22, 17], "US Letter paper"],
			"jisB4" -> [[364, 257], "nearly iso"],
			"usMemo" -> [[17, 11], "Associated Music Publishers (AMP) mini scores"],
			"r25_19" -> [[25, 19], "usParts"],
			"r13_10" -> [[13, 10], "usParts"],
			"r2_1" -> [[2, 1], "octave"],
			"r15_8" -> [[15, 8], "major 7th"],
			"r5_3" -> [[5, 3], "major 6th"],
			"r8_5" -> [[8, 5], "minor 6th"],
			"r6_5" -> [[6, 5], "minor 3rd"],
			"r5_4" -> [[5, 4], "major 3rd"],
			"r7_5" -> [[7, 5], "just intonation tritone alternative"],
			"r9_8" -> [[9, 8], "whole tone"],
			"r16_15" -> [[16, 15], "just intonation minor semitone"],
			"r18_17" -> [[18, 17], "equal temperament semitone"],
			"r25_24" -> [[25, 24], "just intonation major semitone"],
			"r3sqrt" -> [[433, 250], "3.sqrt"]
		]);
	}

	*new {
		^super.new.init;
	}

	init {
		screenDims = this.getScreenSize;
		ratiosDict = MITHScreenRatios.ratiosDict;
		maxDims =  screenDims; // default for initial view
		viewDims = screenDims/10; // default for initial view
		^this
	}

	// **************** Dictionary Management ****************

	// Add or replace dictionary entries, strictly using the [[num, denom], "description"] format
	addRatioDict { |dict, replace=false|
		var newDict;

		// empty dictionary if replacing, otherwise use existing
		newDict = if(replace) {
			Dictionary.new
		} {
			ratiosDict.copy
		};

		// process each entry in the input dictionary
		dict.keysValuesDo({ |key, value|
			// format must be [[num, denom], "description"]
			if(value.isKindOf(Array) and: {
				value[0].isKindOf(Array) and: { value[0].size == 2 }
			}) {
				// add to dictionary
				newDict.put(key.asString, value);
			} {
				format("Invalid ratio format for key '%'. Expected [[num, denom], \"description\"]. Skipping.", key).postln;
			};
		});

		// Update instance dictionary
		ratiosDict = newDict;

		^this;
	}

	addRatio {|key, ratioArr, description=""|
		var ratio;

		if(key.isString.not and: {key.isSymbol.not}){
			"Adding a ratio requires a key String or Symbol".error;
			^nil
		};

		ratio = this.ratio(ratioArr);
		// add to dictionary - [ratio array, description]
		ratiosDict = ratiosDict.add(key.asString -> [ratio, description]);
		^this
	}

	removeRatio { |key|
		ratiosDict.removeAt(key);
		^this
	}

	// returns Association
	ratioObject { |key|
		key = key.asString;
		^ratiosDict.associationAtFail(key, {
			"No ratio item at that key".error
			^nil
		})
	}

	// **************** Ratio Operations ****************

	// lookup only
	getRatio { |ratioName|
		var result = ratiosDict[ratioName.asString];
		if(result.notNil) {
			^result[0];
		} {
			"No ratio with that name found".error;
			^nil;
		};
	}

	// reduce ratio only, can handl ratios with floats, e.g. .ratio([4.2, 3.8]) // [21, 19]
	reduceRatio { |ratioArray|
		var arr, gcdVal, factor = 1;

		if(ratioArray.isKindOf(Collection) and: {ratioArray.size==2}) {
			// find scaling factor, up to 5 decimal place
			5.do { |i|
				factor = 10 ** i;
				if((ratioArray[0] * factor).frac < 0.01 and: { (ratioArray[1] * factor).frac < 0.01 }) {
					// found factor
					^this.prProcessScaledRatio(ratioArray, factor);
				};
			};

			// default case - use highest tested factor
			^this.prProcessScaledRatio(ratioArray, 10000);
		} {
			"Invalid ratio format. Expected [num, denom] array.".error;
			^nil;
		};
	}

	// Helper method to process the scaled ratio
	prProcessScaledRatio { |ratioArray, factor|
		var arr, gcdVal;

		arr = ratioArray.collect{|i| (i * factor).round.asInteger};

		if(arr.includes(0)) {
			"Cannot reduce ratio with zero element".error;
			^nil;
		};

		arr = arr.sort.reverse;
		gcdVal = arr[0].gcd(arr[1]);
		arr = (arr / gcdVal).asInteger;
		^arr;
	}

	// polymorphism
	ratio { |input|
		if(input.isString or: {input.isKindOf(Symbol)}) {

			^this.getRatio(input);
		} {
			^this.reduceRatio(input);
		};
	}

	// ratio-to-decimal conversion
	ratioToDecimal { |arr, precision=4|
		var decPrecision;

		// convert precision int to decimal places
		decPrecision = 1 / (10 ** precision);

		arr = arr.copy.sort.reverse; // ensure largest first
		^(arr[1]/arr[0]).round(decPrecision)
	}


	// decimal-to-ratio conversion / rational approximation
	// translate from Float to ratio Array
	asRatio { |decimal=1.0, maxDenominator=100|
		var frac, num, denom, result;

		// NB: asFraction returns [denominator, divisor]
		frac = decimal.abs.asFraction(maxDenominator, true);

		denom = frac[0];
		num = frac[1];

		// larger number is first
		result = if(num >= denom) {
			[num, denom]
		} {
			[denom, num]
		};

		// reduce the ratio to lowest terms
		^this.reduceRatio(result);
	}

	// see also ratioToDecimal
	ratioToCents { |ratio|
		var ratioArray, num, denom, frequencyRatio;

		// allow different input formats
		ratioArray = case
		{ ratio.isKindOf(Symbol) } { this.ratioObject.value[0] }
		{ ratio.isKindOf(Array) and: {ratio.size==2}} { ratio };

		if(ratioArray.notNil){
			ratioArray = ratioArray.copy.sort.reverse;
			num = ratioArray[0];
			denom = ratioArray[1];
			frequencyRatio = num / denom;

			// calculate cents using the formula: cents = 1200 × log₂(frequency ratio)
			^(1200 * (frequencyRatio.log / 2.log)).round.asInteger;
		}{
			("Invalid ratio name or Array size").error;
			^nil
		}
	}

	// convert cents to ratio
	centsToRatio { |cents, maxDenominator=100|
		var frequencyRatio;

		cents = cents.asFloat;

		// Calculate frequency ratio from cents using the formula: ratio = 2^(cents/1200)
		frequencyRatio = 2.pow(cents/1200);

		// Use existing asRatio method to find the closest integer ratio
		^this.asRatio(frequencyRatio, maxDenominator);
	}

	//difference in cents between two ratios
	diffInCents { |arr1, arr2, precision=2|
		var cents1, cents2, difference, decPrecision;
		var ratio1, ratio2;

		// use normalised arrays
		arr1 = this.ratio(arr1);
		arr2 = this.ratio(arr2);

		// frequency ratios (larger/smaller)
		ratio1 = arr1[0] / arr1[1];
		ratio2 = arr2[0] / arr2[1];

		// cents = 1200 * log2(ratio)
		cents1 = 1200 * (ratio1.log / 2.log);
		cents2 = 1200 * (ratio2.log / 2.log);

		// absolute difference
		difference = (cents1 - cents2).abs;

		// precision
		decPrecision = 1 / (10 ** precision);
		^difference.round(decPrecision);
	}

	ratioDescr {|name|
		var descr, ratio = this.ratiosDict[name.asString];
		if(ratio.notNil){
			descr = ratio[1];
			if(descr.notEmpty){
				^ratio[1] // Get description from [ratioArray, description]
			}{
				"No description present".warn;
				^nil
			}
		}{
			^nil
		}
	}

	// **************** Search and Analysis ****************

	// returns Dictionary
	searchRatios { |searchTerm, printResults=true|
		var results, isNumeric, termNum;
		var termStr, found, ratioArr;

		results = Dictionary.new;
		isNumeric = false;
		termNum = nil;
		termStr = searchTerm.asString;

		// check if search term numeric
		if(searchTerm.isKindOf(Number) or: {
			searchTerm.asString.every({ |char| char.isDecDigit })
		}) {
			isNumeric = true;
			termNum = searchTerm.asString.asInteger;
		};

		ratiosDict.keysValuesDo({ |key, value|
			found = false;
			ratioArr = value[0]; // Get ratio array from [ratioArray, description]

			// for numeric searches, only match exact integers
			if(isNumeric) {
				// ensure numeric comparison by converting both sides
				if(ratioArr[0].asInteger == termNum or: { ratioArr[1].asInteger == termNum}) {
					found = true;
				};

				// Special case for "square" when searching for 1
				if(key == "square" && termNum == 1) {
					found = true;
				};

				// Special case for keys that explicitly reference this number
				if(key.findRegexp("r?" ++ termNum ++ "_|_" ++ termNum ++ "$").size > 0) {
					found = true;
				};
			};

			// for text searches, do normal substring matching
			if(isNumeric.not) {
				if(key.toLower.contains(termStr.toLower) or:
					value[1].toLower.contains(termStr.toLower) or: // Check description
					ratioArr[0].asString.contains(termStr) or:
					ratioArr[1].asString.contains(termStr)) {
					found = true;
				};
			};

			// add to results if found
			if(found) {
				results = results.add((key -> value));
			};
		});

		// print summary
		if(printResults) {
			if(results.size > 0) {
				("Found " ++ results.size ++ " matching ratios:").postln;
				this.listRatios(results.asAssociations)
			} {
				"No matching ratios found.".postln;
			};
		};

		^results;
	}

	closestRatio {|ratio, precision=4, highPrecision=false|
		var targetDecimal, closestKey, closestDist;
		var result, reducedRatio, distance;
		var ratioArray, decimalValue;

		// decimal calculation with precision mode
		if(highPrecision) {
			targetDecimal = ratio[1] / ratio[0];
		} {
			reducedRatio = this.ratio(ratio);
			targetDecimal = this.ratioToDecimal(reducedRatio, precision);
		};

		// initialize with large distance
		closestDist = 100.0;

		// find closest match
		ratiosDict.keysValuesDo { |key, value|
			ratioArray = value[0];
			decimalValue;

			// calculate decimal value based on mode
			decimalValue = if(highPrecision) {
				ratioArray[1] / ratioArray[0];
			} {
				this.ratioToDecimal(ratioArray, precision);
			};

			distance = (targetDecimal - decimalValue).abs;

			if(distance < closestDist) {
				closestDist = distance;
				closestKey = key;
			};
		};

		// format output based on mode
		if(highPrecision) {
			format("High precision comparison: % (%)", ratio, targetDecimal).postln;
		} {
			format("Ratio of % is % (%)", ratio, reducedRatio, targetDecimal).postln;
		};

		format("Closest match distance: %", closestDist).postln;

		// get result and optionally display details
		result = this.ratioObject(closestKey);
		if(highPrecision.not) {
			this.postRatioArr(this.ratioObjectData(result, precision));
		};

		^result
	}

	// returns an array including ratio data as decimal and in cents
	ratioObjectData { |ratioObject, precision=4|
		var key, value, ratio, descr, dec, cents;

		key = ratioObject.key;
		value = ratioObject.value;

		// ratio array and description
		ratio = value[0];
		descr = value[1];

		// Use the provided precision
		dec = this.ratioToDecimal(ratio, precision);
		cents = this.ratioToCents(ratio);
		^[key, ratio, descr, dec, cents]
	}

	// **************** Display / posting ****************

	// Array elements have form:
	// e.g. [ r18_17, [[18, 17], "equal temperament semitone"] ]
	sortRatios { |array, sortIndex = 3, precision=4|
		array = array ?? { this.ratiosDict.asAssociations };
		// transform ratio object Association into [key, ratio, descr, dec, cents]
		array = array.collect{|item| this.ratioObjectData(item, precision)}
		// dy default sorting is by decimal representation of the ratio
		^array.sort({arg a, b;
			a[sortIndex]> b[sortIndex]
		});
	}

	// pretty printing
	postRatioArr {|ratioArr|
		// [key, ratio, descr, dec, cents]
		("\\"++format("% : % \"%\" % | % cents",
			ratioArr[0], ratioArr[1], ratioArr[2], ratioArr[3], ratioArr[4])
		).postln;
	}

	// pretty printing
	listRatios { |array|
		var data;
		array = array ?? { this.ratiosDict.asAssociations };
		array = this.sortRatios(array); // sorts by decimal
		array.do{|item| this.postRatioArr(item)}; // post
		^this
	}

	// from ratio name
	postRatio { |name|
		var obj, data;
		obj = this.ratioObject(name); // will post error as needed
		if(obj.notNil){
			data = this.ratioObjectData(obj);
			this.postRatioArr(data); // post
			^obj
		}{
			^this
		}
	}

	// **************** GUI / Screen Methods ****************

	gui { |win|
		^MITHRatiosView.new(this, win)
	}

	resetDims {
		viewDims = screenDims;
		this.changed(\viewDims, viewDims);
		^viewDims
	}

	maxDims_{|arr|
		^maxDims = this.checkDims(arr);
	}

	checkDims {|arr|
		if(arr.isKindOf(SimpleNumber)){
			arr = arr.dup
		};
		^arr.asInteger
	}

	viewDims_ {|arr|
		viewDims = this.checkDims(arr);
		this.changed(\viewDims, viewDims);
		^this
	}

	ratioToDims {|ratio, dims, landscape=true|
		var newDims, dimX, dimY, test;
		if(ratio.isNil){ratio = \r16_9};
		dims = if(dims.isNil){
			maxDims
		}{
			this.checkDims(dims)
		};
		dimX = dims[0];
		dimY = dims[1];
		ratio = this.ratio(ratio);
		if(ratio.isArray){
			ratio = ratio.sort.reverse; // largest num always first
			if(landscape.not){ratio = ratio.reverse};
			test = (dimX * (ratio[1]/ratio[0])).round <= dimY;
			newDims = if(test){
				[dimX, (dimX * (ratio[1]/ratio[0]))];
			}{
				[(dimY * (ratio[0]/ratio[1])), dimY];
			};
		}{
			"Ratio symbol key does not exist in the ratio Dictionary".error;
			^nil
		};
		^newDims.round.asInteger;
	}

	getScreenSize {
		screenDims = Window.screenBounds;
		screenDims = [screenDims.width, screenDims.height];
		^screenDims.asInteger
	}

	resizeWin {|dims| // Array
		dims = dims ?? {screenDims/10};
		^this.viewDims_(dims)
	}
}

MITHRatiosView {

	var <model, <win;

	*new { |model, win|
		^super.new.init(model, win);
	}

	init {|argModel, argWin|
		model = argModel;
		model.addDependant(this);
		win = argWin;
		if(win.isNil){
			win = Window.new.front;
			this.makeWindow(model.viewDims, win);
		};
		win.onClose_({ model.removeDependant(this)});
		^win
	}

	makeWindow  {|dims, argWin|
		var newWidth, newHeight, tmpBounds, width, height, drawBounds;
		width = dims[0];
		height = dims[1];
		win = argWin;
		win.name_(format("[%, %]", width, height));
		win.setTopLeftBounds(Rect(0, 0, width, height));
		win.drawFunc = {|self|
			Pen.color = Color.new255(255, 85, 0); // orange
			Pen.addRect(
				Rect(1, 1, self.bounds.width-2, self.bounds.height-2)
			);
			Pen.width_(2);
			Pen.stroke;
		};
		win.refresh;

		win.view.onResize = {|self|
			tmpBounds = self.bounds;
			newWidth = tmpBounds.width;
			newHeight = tmpBounds.height;
			model.viewDims = [newWidth, newHeight]; // CONTROLLER FN
			win.name_(format("[%, %]", newWidth.asInteger, newHeight.asInteger));
		};
		^win;
	}

	update {|obj, what, val|
		case {what == \viewDims} {
			win.setInnerExtent(val[0], val[1])
		};
		^this
	}


}
