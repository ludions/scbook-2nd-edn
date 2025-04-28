
/*
ScreenRatios
ScreenRatiosView

© 2022 Tom Hall

*/

MITHScreenRatios {
	var  <>screenDims, <viewDims, <ratiosDict, <maxDims;

	// see also Robert Bringhurst, 1999, p.147
	// The Elements of Typographical Style
	// and e.g. https://mola-inc.org/resources/11631
	*ratiosDict {
		^Dictionary.with(*[
			"square" -> [1, 1], // unison
			"r16_9" -> [16, 9], // min7th
			"r4_3" -> [4, 3], // p4th
			"r3_2" -> [3, 2], // p5th
			"iso" -> [14142, 10000], // dim5th / aug4th / sqrt(2)
			"phi" -> [1618, 1000], // (1 + sqrt(5))/2
			"usLetter" -> [22, 17],
			"jisB4" -> [364, 257],  // nearly iso
			"usMemo" -> [17, 11], // e.g AMP mini scores (Babbitt Str. Qrt 2)
			"r25_19" -> [25, 19], // usParts
			"r13_10" ->[13, 10], // // usParts
			"r2_1" ->[2, 1], // octave
			"r15_8" ->[15, 8], // maj7th
			"r5_3" ->[5, 3], // maj6th
			"r8_5" ->[8, 5], // min6th
			"r6_5" ->[6, 5], // min3rd
			"r5_4" ->[5, 4], // maj3rd
			"r7_5" ->[7, 5],
			"r9_8" ->[9, 8],
			"r16_15" ->[16, 15],
			"r3sqrt" -> [1732, 1000] // 3.sqrt
		]);
	}

	*ratio {|ratio, dict|
		var arr, rtnRatio;
		if(ratio.isString or: {ratio.isKindOf(Symbol)}){
			rtnRatio = ratio.asString;
			if(dict.isNil){dict = this.ratiosDict};
			rtnRatio = dict[rtnRatio];
		};
		if(ratio.isKindOf(Collection) and:{ratio.size==2}){
			rtnRatio = ratio.asInteger.sort.reverse;
			rtnRatio = (rtnRatio / rtnRatio[0].gcd(rtnRatio[1])).asInteger;
		};
		^rtnRatio
	}

	*new {
		^super.new.init;
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

	addRatio {|key, ratioArr|
		var ratio = this.ratio(ratioArr);
		ratiosDict.add(key.asString -> ratio);
		^this
	}

	ratiosListWithFloats {
		var list, float;
		list = this.sortRatios;
		list = list.collect({|i|
			float = ((i[1][1]) / (i[1][0])).round(0.001);
			[i[0], i[1], float]
		});
		^list
	}

	listRatios {
		var list, float;
		list = this.ratiosListWithFloats;
		list.do({|i|
			("\\"++format("% : %  (%)", i[0], i[1], i[2])).postln
		});
		^this
	}

	sortRatios {
		^this.ratiosDict.asSortedArray.sort({arg a, b;
			(a[1][1]/a[1][0]) > (b[1][1]/b[1][0])
		});
	}

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
		float = this.decimal(*reducRatio);
		rFlList = this.decimalRatios.collect{|i, j| i[1]};
		matchFloat = float.nearestInList(rFlList.reverse);
		matchDist = (float - matchFloat).abs;
		rtn = this.ratiosListWithFloats[rFlList.indexOf(matchFloat)];
		format("Ratio of % is % (%)", ratio, reducRatio, float.round(0.001)).postln;
		format("Distance of % from: ", matchDist.round(0.001)).postln;
		this.printFancyRatio(rtn);
		^rtn[1]
	}

	decimal {|a, b|
		var arr = [a, b].sort.reverse; // largest first
		^(arr[1]/arr[0])
	}

	decimalRatios {
		var list, float;
		list = this.sortRatios;
		^list.collect{|i| [i[0], this.decimal(i[1][1], i[1][0]).round(0.001)]}
	}

	ratiosList {^this.listRatios}

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
		ratio = MITHScreenRatios.ratio(ratio, ratiosDict);
		if(ratio.isArray){
			ratio = ratio.sort.reverse; // largest num always first
			if(landscape.not){ratio = ratio.reverse};
			test = (dimX * (ratio[1]/ratio[0])).round <= dimY;
			newDims = if(test){
				[dimX, (dimX * (ratio[1]/ratio[0]))];
			}{
				//if((dimY * (ratio[0]/ratio[1])).round <=dimX)
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

	ratio {|ratio|
		^MITHScreenRatios.ratio(ratio, ratiosDict);
	}

	r {|ratio|
		^this.ratio(ratio)
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
		//model.viewDims = [newWidth, newHeight]; // CONTROLLER FN
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
