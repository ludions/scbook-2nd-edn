
/*
MITHScreenRatios
MITHScreenRatiosView

Dependency: RatioArray

© 2022 - 2025 Tom Hall
ludions.com

*/

MITHScreenRatios {
	var  <>screenDims, <viewDims, <ratiosDict, <maxDims;

	// see also Robert Bringhurst, 1999, p.147
	// The Elements of Typographical Style
	// and e.g. https://mola-inc.org/resources/11631

	*ratiosDict {
		^Dictionary.with(*[
			"square" -> RatioArray([1, 1], "unison"),
			"r16_9" -> RatioArray([16, 9], "minor 7th"),
			"r4_3" -> RatioArray([4, 3], "perfect 4th"),
			"r3_2" -> RatioArray([3, 2], "perfect 5th"),
			"iso" -> RatioArray([7071, 5000], "tritone / sqrt(2)"),
			"phi" -> RatioArray([809, 500], "(φ: (1 + sqrt(5))/2"),
			"usLetter" -> RatioArray([22, 17], "US Letter paper"),
			"jisB4" -> RatioArray([364, 257], "nearly iso"),
			"usMemo" -> RatioArray([17, 11], "Associated Music Publishers (AMP) mini scores"),
			"r25_19" -> RatioArray([25, 19], "usParts"),
			"r13_10" -> RatioArray([13, 10], "usParts"),
			"r2_1" -> RatioArray([2, 1], "octave"),
			"r15_8" -> RatioArray([15, 8], "major 7th"),
			"r5_3" -> RatioArray([5, 3], "major 6th"),
			"r8_5" -> RatioArray([8, 5], "minor 6th"),
			"r6_5" -> RatioArray([6, 5], "minor 3rd"),
			"r5_4" -> RatioArray([5, 4], "major 3rd"),
			"r7_5" -> RatioArray([7, 5], "just intonation tritone alternative"),
			"r9_8" -> RatioArray([9, 8], "whole tone"),
			"r16_15" -> RatioArray([16, 15], "just intonation minor semitone"),
			"r18_17" -> RatioArray([18, 17], "equal temperament semitone"),
			"r25_24" -> RatioArray([25, 24], "just intonation major semitone"),
			"r3sqrt" -> RatioArray([433, 250], "3.sqrt")
		]);
	}


	*new {
		^super.new.init;
	}

	ratio { |ratio|
		var arr, result, gcdValue;

		case
		{ratio.isString or: {ratio.isKindOf(Symbol)}} {
			ratio = ratio.asString;
			result = ratiosDict[ratio];

			// convert RatioArray to plain array for compatibility
			if(result.notNil) {
				^result.asArray  // return array, not RatioArray
			} {
				"No ratio with that name found".error;
				^nil
			};
		}
		// array input delegates to reduceRatio
		{ratio.isKindOf(Collection)} {
			^this.reduceRatio(ratio);
		};

		"No ratio found".error;
		^nil  // no valid ratio found

	}

	reduceRatio { |ratio|
		var arr, gcdVal;

		// simplify the ratio
		if(ratio.isKindOf(Collection) and: {ratio.size==2}) {
			arr = ratio.collect{|i| i.round.asInteger}; // in case of Floats
			// normalise ratio
			arr = arr.sort.reverse;
			gcdVal = arr[0].gcd(arr[1]);
			if(gcdVal == 0) {
				"Cannot reduce ratio with zero element".error;
				^nil;
			};
			arr = (arr / gcdVal).asInteger;
			^arr;
		}{
			^nil // no valid ratio found
		};
	}


	ratioToCents { |ratio|
		var ratioArray, num, denom, frequencyRatio;

		// Handle different input formats
		ratioArray = case
		{ ratio.isKindOf(Symbol) } { this.getRatioObject(ratio) }
		{ ratio.isKindOf(Array) and: {ratio.size==2}} { ratio };

		if(ratioArray.isKindOf(RatioArray)){
			ratioArray = ratioArray.ratio
		};


		if(ratioArray.notNil){

			ratioArray = ratioArray.sort.reverse;
			num = ratioArray[0];
			denom = ratioArray[1];
			frequencyRatio = num / denom;

			// calculate cents using the formula: cents = 1200 × log₂(frequency ratio)
			^(1200 * (frequencyRatio.log / 2.log)).round.asInteger;
		}{

			("Invalid ratio name or Array size").error;
			^this
		}
	}


	asRatio { |decimal=1.0, maxDenominator=100|
		var frac, num, denom;

		// NB: asFraction returns [denominator, divisor]
		frac = decimal.abs.asFraction(maxDenominator, true);

		denom = frac[0];
		num = frac[1];

		// larger number is first for consistency
		^if(num >= denom) {
			[num, denom]
		} {
			[denom, num]
		};
	}



	getRatioObject {|name|
		^ratiosDict[name.asString];
	}

	resetDims {
		viewDims = screenDims;
		this.changed(\viewDims, viewDims);
		^viewDims
	}

	init {
		screenDims = this.getScreenSize;
		ratiosDict = MITHScreenRatios.ratiosDict;
		maxDims =  screenDims; // default for initial view
		viewDims = screenDims/10; // default for initial view
		^this
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

	addRatio {|key, ratioArr, description=""|
		var ratio;

		if(key.isString.not and: {key.isSymbol.not}){
			"adding a ratio requires a key String or Symbol".error;
			^this
		};

		if(ratioArr.isKindOf(RatioArray)) {
			// already a RatioArray, use it directly
			ratio = this.ratio(ratioArr);
		} {
			// create a new RatioArray
			ratio = RatioArray(
				this.ratio(ratioArr).asArray,
				description
			);
		};

		// add to dictionary
		ratiosDict = ratiosDict.add(key.asString -> ratio);
		^this
	}

	ratioDescr {|name| ^this.ratioDescription(name)}

	ratioDescription {|name|
		var ratio = this.ratiosDict[name.asString];
		if(ratio.notNil){
			^ratio.description
		}{
			^nil
		}
	}

	ratiosListWithFloats { |array|
		var list, float, key, ratioArr;
		array = array ?? { this.ratiosDict.asSortedArray };
		list = this.sortRatios(array);
		list = list.collect({ |item|
			key = item[0];
			ratioArr = item[1];
			float = this.asDecimal(ratioArr[0], ratioArr[1]);
			[key, ratioArr, float]
		});
		^list
	}


	searchRatios { |searchTerm, printResults=true|

		var results, isNumeric, termNum;
		var termStr, found, ratioArr;

		results = Array.new;
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
			ratioArr = value.ratio;

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
					value.description.toLower.contains(termStr.toLower) or:
					ratioArr[0].asString.contains(termStr) or:
					ratioArr[1].asString.contains(termStr)) {
					found = true;
				};
			};

			// add to results if found
			if(found) {
				results = results.add([key, value]);
			};
		});

		// print summary
		if(printResults) {
			if(results.size > 0) {
				("Found " ++ results.size ++ " matching ratios:").postln;
				this.listRatios(results)
			} {
				"No matching ratios found.".postln;
			};
		};

		^results;
	}

	// pretty printing
	listRatios { |array|
		var list, float, cents, ratio;
		array = array ?? { this.ratiosDict.asSortedArray };
		list = this.ratiosListWithFloats(array);
		list = list.collect{|item|
			ratio = item[1].asArray;
			cents = this.ratioToCents(ratio);
			[item[0], ratio, item[1].description, item[2], cents]
		};
		list.do({|item|
			("\\"++format("%  % \"%\" % | cents: %", item[0], item[1], item[2], item[3], item[4])).postln
		});
		^list
	}

	// Array elements have form:
	// e.g. [ r18_17, RatioArray([ 18, 17 ] | "equal temperament semitone") ]
	sortRatios { |array|
		array = array ?? { this.ratiosDict.asSortedArray };
		^array.sort({arg a, b;
			this.asDecimal(a[1][0], a[1][1]) > this.asDecimal(b[1][0], b[1][1])
	});	}

	printFancyRatio {|ratio|
		("\\"++format("% : %  (%)", ratio[0], ratio[1], ratio[2])).postln;
		^this
	}

	closestMatch  { |ratio|
		^this.closestRatio(ratio)
	}

	closestRatio {|ratio|
		var rtn, float, rFlList, matchFloat, reducRatio, matchDist;
		reducRatio = this.ratio(ratio);
		float = this.asDecimal(*reducRatio);
		rFlList = this.asDecimalRatios.collect{|i, j| i[1]};
		matchFloat = float.nearestInList(rFlList.reverse);
		matchDist = (float - matchFloat).abs;
		rtn = this.ratiosListWithFloats[rFlList.indexOf(matchFloat)];
		format("Ratio of % is % (%)", ratio, reducRatio, float.round(0.001)).postln;
		format("Distance of % from: ", matchDist.round(0.001)).postln;
		this.printFancyRatio(rtn);
		^rtn[1]
	}

	asDecimal {|num, denom|
		var arr = [num, denom].sort.reverse; // ensure largest first
		^(arr[1]/arr[0]).round(0.001)
	}

	decimalRatios {
		var list, float;
		list = this.sortRatios;
		^list.collect{|i| [i[0], this.asDecimal(i[1][1], i[1][0]).round(0.001)]}
	}

	rToDims {|ratio, dims, landscape=true|
		^this.ratioToDims(ratio, dims, landscape)
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
			^"Ratio symbol key does not exist in the ratio Dictionary".error
		};
		^newDims.round.asInteger;
	}

	getScreenSize {
		screenDims = Window.screenBounds;
		screenDims = [screenDims.width, screenDims.height];
		^screenDims.asInteger
	}

	resizeWin {|dims| // Array
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
