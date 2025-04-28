
/*
NotnFlow
© 2022 Tom Hall
Dependencies:
- TextModel
- WinBlock and/or ViewCentral

version 2022-04
*/


NotnFlowTitleModel : NotnFlowModel {
	var <subtitle, <subsubtitle;

	makeGuiModels {
		super.makeGuiModels;
		subtitle = TextModel.new;
		subtitle.addDependant(this);
		subsubtitle = TextModel.new;
		subsubtitle.addDependant(this);
	}

	viewCol_{|color|
		super.viewCol_(color);
		subtitle.background_(color); // update model
		subsubtitle.background_(color); // update model
	}

	update {|obj, what, val|
		// ["NotnFlowTitleModel update:", obj, what, val].postln;
		super.update(obj, what, val);
		case {obj == subtitle} {
			this.changed(what, val, \subtitle);
		}
		{obj == subsubtitle} {
			this.changed(what, val, \subsubtitle);
		}
	}

}


NotnFlowModel {

	var <numSystems, <viewCol, <sysTopTexts, <systemNotnsHeightPx;
	var <leftTitle, <midGutter, <rightGutter, <leftGutter;
	var <composer, <title, <vewModel, <viewDims, <systemWidths;
	var <systemNotns, <systemTopsHeight, <systemTxtsFn;
	var <layoutSpacing, <systemTxts, <systemWidthsPx, <sysTxtFnDict;
	var <guttersHeight, <systemInstrs, <systemWidthsTxtPx;

	// model is ViewCentral model
	*new { |viewModel, numSystems|
		^super.new.init(viewModel, numSystems);
	}


	init {|argViewModel, argNumSystems|
		vewModel = argViewModel;
		vewModel.addDependant(this);
		viewDims = vewModel.viewDims;
		numSystems = argNumSystems ?? 1;
		sysTxtFnDict = Dictionary.new;
		viewCol = Color.grey(0.95);
		this.makeGuiModels;
		this.layoutSpacing_(2); // 6 is otherwise default
		systemWidths = [0.75, 0.85]; // default
		this.setSystemWidths(*systemWidths);
		^this;
	}

	makeGuiModels {|objects|

		sysTopTexts = Dictionary.new;
		numSystems.do{|i| sysTopTexts.add(i -> (i ->TextModel.new))};

		systemNotns = Dictionary.new;
		numSystems.do{|i| systemNotns.add(i -> (i ->GuiModel.new))};

		systemNotns.do({|i|
			i.value.addDependant(this);
		});

		sysTopTexts.do({|i|
			i.value.addDependant(this);
			i.value.align_(\left);
		});

		title = TextModel.new;
		title.addDependant(this);
		title.fontWeightBool_(true);

		leftTitle = TextModel.new;
		leftTitle.addDependant(this);
		leftTitle.align_(\left);

		midGutter = TextModel.new;
		midGutter.addDependant(this);
		midGutter.align_(\bottom);

		rightGutter = TextModel.new;
		rightGutter.addDependant(this);
		rightGutter.align_(\bottomRight);

		leftGutter = TextModel.new;
		leftGutter.addDependant(this);
		leftGutter.align_(\bottomLeft);

		composer = TextModel.new;
		composer.addDependant(this);
		composer.align_(\bottomRight);

		^this;
	}


	viewCol_{|color|
		viewCol = color;
		title.background_(viewCol); // update model
		composer.background_(viewCol);
		leftTitle.background_(viewCol);
		leftGutter.background_(viewCol);
		midGutter.background_(viewCol);
		rightGutter.background_(viewCol);

		systemNotns.do({|i|
			i.value.background_(viewCol);
		});

		sysTopTexts.do({|i|
			i.value.background_(viewCol);
		});

		// change view of areas not accessible in model
		this.changed(\viewCol, viewCol);
	}

	layoutSpacing_ {|pixels = 2|
		layoutSpacing = pixels;
		this.changed(\layoutSpacing, layoutSpacing);
		^this
	}

	// for uniformity
	setLayoutSpacing_ {|pixels = 2|
		this.layoutSpacing_(pixels);
		^this;
	}


	systemTopText {|int|
		^sysTopTexts.tryPerform(\at, int).value
		//^sysTopTexts[0].value
	}

	systemInstrs_ {|first, subseq, font, color|
		^this.setSystemTxts(first, subseq, font, color)
	}

	// convenience method for single instrument score
	// more complex system texts should use sysTxtFnEval
	setSystemTxts {|first, subseq, font, color, fn|
		var text;

		if (color.isNil) {color = Color.black};
		systemTxtsFn = fn;
		if(systemTxtsFn.isNil){
			systemTxtsFn = {|txt, font, color| {|self|
				var bounds, rect, height;
				bounds = self.bounds;
				rect = Rect(0, 0, bounds.width, bounds.height);
				// txt.drawCenteredIn(rect, font, color);
				txt.drawLeftJustIn(rect, font, color)
			}
			}
		};

		systemTxts = [first, subseq, font, color, systemTxtsFn];
		systemInstrs = systemTxts;

		numSystems.do{|i|
			text = [first, subseq][i.min(1)];
			this.sysTxtFnEval(i, systemTxtsFn, text, font, color);
		};
	}

	sysTxtFnEval  {|usrViewInt, fn ... args|
		//if(usrViewInt<=1){
		sysTxtFnDict.put(usrViewInt, [fn] ++ args);
		//};
		this.changed(\sysTxtFnEval, [usrViewInt, fn] ++ args);
		^this
	}

	setSystemWidths {| first=0.75, subseq=0.85|
		systemWidths = [first, subseq];
		systemWidthsPx = systemWidths.copy;
		if(systemWidthsPx.sum<2){
			systemWidthsPx = systemWidthsPx
			* (viewDims[0] - layoutSpacing);
			systemWidthsPx = systemWidthsPx.round(0.01)
		};
		numSystems.do({|i|
			sysTopTexts[i].value.fixedWidth_(systemWidthsPx[i.min(1)]);
			systemNotns[i].value.fixedWidth_(systemWidthsPx[i.min(1)]);
		});
		this.calcSystemTxtWidths; // updates systemWidthsTxtPx
		^systemWidthsPx
	}


	// values unused ATM
	calcSystemTxtWidths {
		var viewWidth;
		viewWidth = viewDims[0] - layoutSpacing;
		systemWidthsTxtPx = [viewWidth, viewWidth] - systemWidthsPx;
		^systemWidthsTxtPx;
	}

	setGuttersHeight {| maxHeight |
		var pixels, gutters;
		guttersHeight = maxHeight;
		pixels = guttersHeight.copy;
		if(pixels<1){
			pixels = pixels * viewDims[1]
		};
		gutters = [midGutter, rightGutter, leftGutter];
		gutters.do({|i|
			i.maxHeight_(pixels);
		});
		^[maxHeight, pixels]
	}


	setSystemTopsHeight {| height |
		var pixels;
		systemTopsHeight = height;
		pixels = systemTopsHeight.copy;
		if(pixels<1){
			pixels = pixels * viewDims[1]
		};
		numSystems.do({|i|
			sysTopTexts[i].value.fixedHeight_(pixels);
		});
		^[height, pixels]
	}

	setSystemNotnsHeight {| heightPx |
		systemNotnsHeightPx = heightPx;
		numSystems.do({|i|
			systemNotns[i].value.fixedHeight_(systemNotnsHeightPx);
		});
		^systemNotnsHeightPx
	}



	update {|obj, what, val|
		//["NotnFlowModel update:", obj, what, val].postln;
		case {obj == leftTitle} {
			this.changed(what, val, \leftTitle);
		}
		{obj == title} {
			this.changed(what, val, \title);
		}
		{obj == composer} {
			this.changed(what, val, \composer);
		}
		{obj == leftGutter} {
			this.changed(what, val, \leftGutter);
		}
		{obj == midGutter} {
			this.changed(what, val, \midGutter);
		}
		{obj == rightGutter} {
			this.changed(what, val, \rightGutter);
		}
		{obj == vewModel and: {what == \viewDims}} {
			viewDims = val;
			this.changed(\viewDims, viewDims);
		};

		numSystems.do{|i|
			case {obj == sysTopTexts[i].value}{
				//format("sysTopText % changed", i).postln;
				this.changed(what, val, \sysTopTexts, i);
			}
			// {obj == systemTxts[i].value}{
			// 	format("systemTxts % changed", i).postln;
			// 	this.changed(what, val, \systemTxts, i);
			// }
			{obj == systemNotns[i].value}{
				//format("systemNotns % changed", i).postln;
				this.changed(what, val, \systemNotns, i);
			}
		}

	}


}


NotnFlowTitle : NotnFlow {
	var <>subtitle, <pageTopTitleLayout, <>subsubtitle;

	setSubtitle {arg text, height = 1.25, width = 1.5;
		subtitle.string_(text);
		^this
	}

	blockCol_{|color|
		super.blockCol_(color);
		subtitle.background_(blockCol);
		subsubtitle.background_(blockCol);
		^this
	}

	makeLayoutItems { |numSystems |

		super.makeLayoutItems(numSystems);

		subtitle = StaticText().align_(\center).background_(blockCol);
		subsubtitle = StaticText().align_(\top).background_(blockCol);

		^this;
	}

	// overrides NotnFlow
	assemblePageTopLayout {
		pageTopTitleLayout = VLayout(title, subtitle, subsubtitle);
		pageTopLayout = HLayout(leftTitle, pageTopTitleLayout, composer);
		^this
	}



	assembleLayout {

		super.assembleLayout;

		this.setTextObject(subtitle, model.subtitle);
		this.setTextObject(subsubtitle, model.subsubtitle);

		^this;
	}

	update {|obj, what, val, objectName, objectInt|

		super.update(obj, what, val, objectName, objectInt);

		case {objectName == \subtitle} {
			subtitle.tryPerform(what, val);
		}
		{objectName == \subsubtitle} {
			subsubtitle.tryPerform(what, val);
		}
	}

}


NotnFlow {
	var <win, <numSystems, <title, <>systems, <>gutter, <>systemTxts;
	var <>systemNotns, <blockCol, <innerVLayout, <>composer, <model;
	var <pageTopLayout, <>leftTitle, <>systemTops, <systemTopLayouts;
	var <pageGutterLayout, <midGutter, <rightGutter, <leftGutter, <model;

	*new { arg model, win;
		^super.new.init(model, win)
	}

	init { arg argModel, aWin;

		model = argModel;
		model.addDependant(this);
		numSystems = model.numSystems;
		win = aWin;
		if(win.isNil, { win = Window.new.front}); // can be a View
		win.onClose_({ model.removeDependant(this)});
		this.makeLayoutItems(numSystems);
		this.assemblePageTopLayout;
		this.assembleLayout;
		this.configureSpacings;
		this.checkSystemTxts;
		^this
	}


	configureSpacings {
		var rhWidthsArr, sysNotnHeightPx;
		rhWidthsArr = model.systemWidthsPx;
		sysNotnHeightPx = model.systemNotnsHeightPx;
		// set system widths
		numSystems.do{|i|
			systemTops[i][1].fixedWidth_(rhWidthsArr[i.min(1)]);
			systemNotns[i].fixedWidth_(rhWidthsArr[i.min(1)]);
		};
		// set system heights if they have been set in the model
		if(sysNotnHeightPx.notNil){
			numSystems.do{|i|
				systemNotns[i].fixedHeight_(sysNotnHeightPx)
			}
		};
		^this;
	}

	// this attempts to update enture StaticText
	// by iterating over state of TextModel
	// this is to avoid separate multiple corresponding method updates
	setTextObject {|argObj, argMod|
		var getterVal, method;
		argMod.class.instVarNames.do{|i|
			getterVal = argMod.tryPerform(i);
			if(getterVal.notNil){
				method = (i.asString++"_").asSymbol;
				if(argObj.respondsTo(method.asSymbol)){
					argObj.tryPerform(method, getterVal);
				}
			};
			// instVarNames will not find TextModel font, as is method
			argObj.tryPerform(\font_, argMod.font);
		};
		^this
	}


	blockCol_{|color|
		blockCol = color;
		// background of instr name area
		// TODO not currently accessible in model
		// other than in global blockCol setter
		systemTxts.do{|i| i.background_(blockCol)};
		// LHS next to system tops color
		// TODO not currently accessible in model
		systemTops.do{|i|
			i[0].background_(blockCol);
		};
		^this
	}

	checkSystemTxts {
		var text, txtData, sysTxtDict;
		sysTxtDict = model.sysTxtFnDict;

		if (sysTxtDict.notEmpty){
			numSystems.do{|i|
				txtData = model.sysTxtFnDict[i];
				txtData.postln;
				// |usrViewInt, fn ... args|
				this.sysTxtFnEval(i, txtData[0], txtData[1], txtData[2]);
			}
		};
		^this;
	}

	// fn is curried, see setSystemTxts in model for example
	sysTxtFnEval  {|usrViewInt, fn ... args|
		systemTxts[usrViewInt].drawFunc = fn.value(*args);
		systemTxts[usrViewInt].refresh;
		^this
	}


	makeLayoutItems { arg aNumSystems;

		numSystems = aNumSystems;
		blockCol = model.viewCol; // Color.grey(0.95);

		leftTitle = StaticText.new;
		title = StaticText.new;
		composer = StaticText.new;

		midGutter = StaticText.new;
		rightGutter = StaticText.new;
		leftGutter = StaticText.new;

		systemTops = numSystems.collect({ [
			UserView().background_(blockCol),
			StaticText()//.background_(blockCol) // updated below
		];
		});

		systemTxts = numSystems.collect({
			UserView.new().background_(blockCol);
		});
		systemNotns = numSystems.collect({UserView.new().background_(blockCol)});

		^this;
	}

	assemblePageTopLayout {
		pageTopLayout = HLayout(leftTitle, title, composer);
	}

	assembleLayout {

		pageGutterLayout = HLayout(leftGutter, midGutter, rightGutter);

		systemTopLayouts = systemTops.collect{|i| HLayout(i[0], i[1])};

		systems = numSystems.collect({|i| HLayout(systemTxts[i], systemNotns[i])});

		// create inner column of title and systems, etc.
		innerVLayout = VLayout.new(pageTopLayout);

		numSystems.do({|i|
			innerVLayout.add(systemTopLayouts[i]);
			innerVLayout.add(systems[i]);
		});

		innerVLayout.add(pageGutterLayout);

		win.layout_(innerVLayout);
		win.layout.margins = 0; // _window_ margins remain 0
		win.layout.spacing_(model.layoutSpacing);

		this.setTextObject(leftTitle, model.leftTitle);
		this.setTextObject(title, model.title);
		this.setTextObject(composer, model.composer);
		this.setTextObject(leftGutter, model.leftGutter);
		this.setTextObject(midGutter, model.midGutter);
		this.setTextObject(rightGutter, model.rightGutter);

		// ensure any exisiting state is updated here at instantiation
		systemTops.do{|i, j|
			this.setTextObject(i[1], model.sysTopTexts[j].value);
		};

		^this
	}


	update {|obj, what, val, objectName, objectInt|

		// ["NotnFlow update:", obj, what, val, objectName, objectInt].postln;

		case {objectName == \sysTopTexts} {
			//["\sysTopTexts", objectInt, what, val].postln;
			systemTops[objectInt][1].tryPerform(what, val);
		}
		{objectName == \systemNotns} {
			//["\systemNotns", objectInt, what, val].postln;
			systemNotns[objectInt].tryPerform(what, val);
		}
		{what == \sysTxtFnEval} {
			//["\sysTxtFnEval", objectInt, what, val].postln;
			this.sysTxtFnEval(*val); //
		}
		{what == \viewCol} {
			blockCol = val;
			this.blockCol_(blockCol);
		}
		{what == \layoutSpacing} {
			win.layout.spacing_(val);
		}
		{objectName == \leftTitle} {
			leftTitle.tryPerform(what, val);
		}
		{objectName == \title} {
			title.tryPerform(what, val);
		}
		{objectName == \composer} {
			composer.tryPerform(what, val);
		}
		{objectName == \leftGutter} {
			leftGutter.tryPerform(what, val);
		}
		{objectName == \midGutter} {
			midGutter.tryPerform(what, val);
		}
		{objectName == \rightGutter} {
			rightGutter.tryPerform(what, val);
		}

	}

}

