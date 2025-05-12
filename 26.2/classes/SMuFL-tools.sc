/*
SMuFLtools

Dependencies:
- MITHUnicode
- a SMuFL font, e.g. Bravura (default) https://github.com/steinbergmedia/bravura/

// check if default font Bravura is installed
Font.availableFonts.collect({|i| (i == "Bravura")}).includes(true)

© 2022–2025 Tom Hall
ludions.com
*/


SMuFLtools {
	var <filePath, <dictGlyphClasses, singleGlyphDict;
	var <searchKeys, allKeys, <glyphName, <searchStr;
	var <glyphCodepoint, <glyphDesc, <>verbose, <fontName;

	*new {|filePath, verbose=false, fontName = "Bravura"|
		^super.new.init(filePath, verbose, fontName);
	}
	init {|afilePath, aVerbose, aFontName|
		var filePathNil;
		filePath = afilePath;
		verbose = aVerbose;
		fontName = aFontName;
		if(filePathNil = filePath.isNil, {
			filePath = this.class.filenameSymbol.asString.dirname.dirname ++ "/data/SMuFL-tools-demo-data.json";
			"Demo json data will be loaded as filePath arg is empty".postln;
		});

		if(File.exists(filePath) and: {this.checkJSON(filePath)}) {
			this.createDictionary(filePath);
		} {
			this.pathErrorMsg;
			if(filePathNil) {
				"File 'SMuFL-tools-demo-data.json' is not in expected place".inform;
			}{
				"Try using pathFinder method to select JSON file path to 'SMuFL-tools-demo-data.json'".inform;
			};
		};
		this.reset;
		^this
	}

	gui {|scale, usePtSize = false|
		SMuFLtoolsGUI.new(this, scale, usePtSize, fontName);
		^this
	}

	pathFinder {
		FileDialog({ |aPath|
			filePath = aPath;
			postln("Selected file:" + filePath);
			if(
				File.exists(filePath) and: {this.checkJSON(filePath)}
			) {
				this.createDictionary(filePath);
			} {
				this.pathErrorMsg;
			};
		},
		fileMode: 0,
		stripResult: true
		);
		^this
	}

	pathErrorMsg {
		"filePath error or JSON files not identified".error;
		"Please try again".inform;
		^this
	}

	checkJSON {|aPath|
		if(aPath.basename.splitext.last.toLower=="json"){
			^true
		}{
			"File type may not be JSON".error.postln;
			^false
		}
	}

	createDictionary {|aFile|
		var file;
		file = File(aFile,"r");
		dictGlyphClasses = file.readAllString.parseYAML;
		allKeys = dictGlyphClasses.keys;
		format("JSON file % loaded", aFile.basename).postln;
		this.reset;
		^this
	}

	reset {
		searchStr = "";
		this.changed(\searchStr, searchStr);
		searchKeys = allKeys.as(Array).sort;
		this.changed(\searchKeys, searchKeys);
		^this
	}

	clear {
		^this.reset;
	}

	search {|str|
		var print = false;
		searchStr = str.asString;
		this.changed(\searchStr, searchStr);
		searchKeys = allKeys.select({ arg item, i; item.containsi(str) });
		if(searchKeys.isEmpty, {
			if(searchStr.notEmpty){
				"No glyph found resulting from that search".warn;
			};
			this.reset;
		}, {
			format("% glyph(s) found.", searchKeys.size).postln;
			searchKeys = searchKeys.as(Array).sort;
			this.changed(\searchKeys, searchKeys);
			if(searchKeys.size==1){
				print = true
			};
			this.setGlyphName(searchKeys[0], print: print);
			if(verbose, {this.printSearchResults});
		});
		^this
	}

	printSearchResults {
		searchKeys.do{|i, j| format("    % \(index: %\)", i, j).postln};
	}

	glyphIndex_ { |index=0|
		if(searchKeys.notEmpty, {
			index = index.min(searchKeys.size-1);
			glyphName = searchKeys[index];
			this.changed(\glyphName, glyphName);
			this.changed(\listViewHighlight, index);
			this.glyphInfo;
		}, {
			"No glyph at that index.".error;
		});
		^this
	}

	selectGlyph {|name|
		name = name.asString;
		if(glyphName != name){
			this.setGlyphName(name, true);
		}
		^this
	}

	setGlyphName { |nameStr, reset = false, print = true|
		if(reset){this.reset}; // clear existing search
		nameStr = nameStr.asString;
		singleGlyphDict = dictGlyphClasses.atFail(nameStr, {
			format("glyphName '%' does not exist. Try using search.", nameStr).error;
			^this
		});
		// avoid repeats
		if(nameStr != glyphName, {
			glyphName = nameStr;
			this.changed(\glyphName, glyphName);
			this.glyphInfo;
			if(print){
				this.postGlyphInfo
			};
		});
		^this
	}

	glyphInfo {
		if(glyphName.notNil, {
			singleGlyphDict = dictGlyphClasses[glyphName];
			glyphCodepoint = singleGlyphDict["codepoint"];
			this.changed(\glyphCodepoint, glyphCodepoint);
			glyphDesc = singleGlyphDict["description"];
			this.changed(\glyphDesc, glyphDesc);
			// format("%, description: %", glyphName, glyphDesc).postln;
			// (glyphName -> glyphCodepoint).asCompileString.postln;
		});
		^this
	}

	postGlyphInfo {
		if(glyphName.notNil){
			if(verbose, {format("%, description: %", glyphName, glyphDesc).postln});
			^(glyphName -> glyphCodepoint).asCompileString.postln;
		}{
			^"No glyph selected"
		}

	}

	postGlyphDict {
		if(glyphName.notNil){
			^(glyphName -> singleGlyphDict).asCompileString.postln
		}{
			^this
		}
	}

}


SMuFLtoolsGUI {
	var model, <win, <>bkgCol, <scale, <usePtSize;
	var <>searchField, <>listView, fontName;
	var <>nameStatic, <>codeStatic, <>descrStatic, <>bigGlyph;

	*new { |model, scale, usePtSize=false, fontName= "Bravura"|
		^super.new.init(model, scale, usePtSize, fontName);
	}

	init { |argModel, argScale, argUsePtSize, aFontName|

		model = argModel;
		model.addDependant(this);
		scale = argScale ?? 1.0;
		usePtSize = argUsePtSize;
		fontName = aFontName;

		bkgCol = Color.grey(alpha:0.1);

		this.makeWin;
		listView.items_(model.searchKeys); // show all names by default

	}


	makeWin {
		// initial dimensions
		var baseWidth = 440;
		var baseHeight = 400;
		var baseSMuFLFontSize = 84;
		var baseUIFontSize = 12;
		var baseColWidth = 220;

		// calculate scaled dimensions
		var scaledHeight, scaledWidth, scaledSMuFLFontSize, scaledUIFontSize, scaledColWidth;
		var widthScaleFactor = 1.0;
		var fontScaleFactor = 1.0;
		var uiFont, bravuraFont;

		// ccaling calculations
		if(scale <= 1) {
			scaledWidth = baseWidth.asInteger;
			scaledHeight = baseHeight.asInteger;
			scaledSMuFLFontSize = baseSMuFLFontSize;
			scaledUIFontSize = baseUIFontSize;
			scaledColWidth = baseColWidth.asInteger;
		} {
			widthScaleFactor = 1.0 + ((scale - 1.0) * 0.5);
			fontScaleFactor = widthScaleFactor;

			scaledWidth = (baseWidth * widthScaleFactor).asInteger;
			scaledHeight = (baseHeight * scale).asInteger;
			scaledColWidth = (baseColWidth * widthScaleFactor).asInteger;

			scaledSMuFLFontSize = (baseSMuFLFontSize * fontScaleFactor).round.asInteger;
			scaledUIFontSize = (baseUIFontSize * fontScaleFactor).round.asInteger;
		};

		// Create font objects with user-specified point size setting
		uiFont = Font(Font.defaultSansFace, scaledUIFontSize, usePointSize: usePtSize);
		bravuraFont = Font(fontName, scaledSMuFLFontSize, usePointSize: usePtSize);

		// make window
		win = Window.new("SMuFL Font GUI",
			Rect(128, 64, scaledWidth, scaledHeight)
		);

		// window layout with scaling
		win.layout = HLayout(
			VLayout(
				[HLayout(
					StaticText(win, scaledColWidth * 0.5 @ (80 * scale))
					.string_("Search")
					.font_(uiFont),
					[searchField = TextField()
						.action_{ arg view;
							model.search(view.value);
							view.string = view.value;
						}
						.font_(uiFont)
						.maxWidth_(scaledColWidth * 0.5),
						align: \left]
				)],
				[listView = ListView(win, scaledColWidth @ (200 * scale))
					.maxWidth_(scaledColWidth)
					.font_(uiFont)
					.background_(Color.white)
					.hiliteColor_(Color.yellow(alpha:0.6))
					.action_({ arg sbs;
						model.setGlyphName(listView.items[sbs.value]);
					})
					.enterKeyAction_({ arg sbs;
						model.setGlyphName(listView.items[sbs.value]);
						model.postGlyphDict
					})
				]
			),
			[VLayout(
				[nameStatic = StaticText()
					.string_("Name: ")
					.font_(uiFont)
					.minSize_(scaledColWidth @ (40 * scale))
					.background_(bkgCol)
					, align: \topLeft],
				[codeStatic = StaticText()
					.string_("Code point:")
					.font_(uiFont)
					.background_(bkgCol)
					.minSize_(scaledColWidth @ (40 * scale))
					, align: \topLeft],
				[descrStatic = StaticText()
					.string_("Description: ")
					.font_(uiFont)
					.background_(bkgCol)
					.minSize_(scaledColWidth @ (80 * scale)),
					align: \topLeft],
				[bigGlyph = StaticText().string_("")
					.font_(bravuraFont)
					.background_(bkgCol)
					.minSize_(scaledColWidth @ (240 * scale))
					, align: \topLeft]
			), align: \top]
		);

		// min win size: restrict shrinking width to 80%, and height to initial value
		win.view.minSize = Size(scaledWidth * 0.8, scaledHeight);

		// max win size: restrict width to initial value, use a large value for height
		win.view.maxSize = Size(scaledWidth, 65535);

		win.front;
	}


	updateGlyphName {|name|
		var listViewIndex;
		nameStatic.string = format("Name:\n%", name);
		// highlight name in list
		//listView.items.postln;
		listViewIndex= listView.items.indexOfEqual(name);
		listView.value_(listViewIndex);
	}

	update {|obj, what, val|
		case
		{what == \glyphName} {
			this.updateGlyphName(model.glyphName)
		}
		{what == \glyphCodepoint} {
			codeStatic.string = format("Code point: %", model.glyphCodepoint);
			// asGlyph uses the Unicode class
			bigGlyph.string = format("        %", model.glyphCodepoint.asGlyph);
		}
		{what == \glyphDesc} {
			descrStatic.string = format("Description: %", model.glyphDesc);
		}
		{what == \searchStr} {
			searchField.string = model.searchStr;
		}
		{what == \searchKeys} {
			listView.items_(model.searchKeys);
		}
		{what == \listViewHighlight} {
			listView.value_(val)
		};

		^this
	}
}














