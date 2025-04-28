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
	var <filePath, <jsonData, <curGlyph;
	var <dictGlyphClasses, <singleGlyphDict;
	var <searchKeys, allKeys, <glyphName, <searchStr;
	var <glyphCodepoint, <glyphDesc, <>verbose;

	*new {|filePath, verbose=false|
		^super.new.init(filePath, verbose);
	}
	init {|afilePath, aVerbose|
		var filePathNil;
		filePath = afilePath;
		verbose = aVerbose;
		if(filePathNil = filePath.isNil, {
			filePath = this.class.filenameSymbol.asString.dirname.dirname ++ "/data/SMuFL-tools-demo-data.json";
			"Demo json data will be loaded as filePath arg is empty".postln;
		});

		if(File.exists(filePath) and: {this.checkJSON(filePath)}) {
			this.creatDictionary(filePath);
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

	gui {|scale|
		SMuFLtoolsGUI.new(this, scale);
		^this
	}

	pathFinder {
		FileDialog({ |aPath|
			filePath = aPath;
			postln("Selected file:" + filePath);
			if(
				File.exists(filePath) and: {this.checkJSON(filePath)}
			) {
				this.creatDictionary(filePath);
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

	creatDictionary {|aFile|
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
		searchStr = str;
		this.changed(\searchStr, searchStr);
		searchKeys = allKeys.select({ arg item, i; item.containsi(str) });
		if(searchKeys.isEmpty, {
			"No glyph found resulting from that search".warn;
			this.reset;
		}, {
			format("% glyph(s) found.", searchKeys.size).postln;
			searchKeys = searchKeys.as(Array).sort;
			this.changed(\searchKeys, searchKeys);
			if(searchKeys.size==1){
				print = true
			};
			this.glyphName_(searchKeys[0], print: print);
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
			this.glyphName_(name, true);
		}
		^this
	}

	glyphName_ { |nameStr, reset = false, print = true|
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
		if(verbose, {format("%, description: %", glyphName, glyphDesc).postln});
		^(glyphName -> glyphCodepoint).asCompileString.postln;

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
	var model, <win, <>bkgCol, <scale;
	var <>searchField, <>listView, col1Width = 220;
	var <>nameStatic, <>codeStatic, <>descrStatic, <>bigGlyph;

	*new { |model, scale|
		^super.new.init(model, scale);
	}
	init { |argModel, argScale|

		model = argModel;
		model.addDependant(this);
		scale = argScale ?? 1.0;

		bkgCol = Color.grey(alpha:0.1);

		this.makeWin;
		listView.items_(model.searchKeys); // show all names by default

	}

	makeWin {
		win = Window.new("SMuFL Font GUI", Rect(128, 64, 440 * scale, 400 * scale)).layout_(
			HLayout(
				VLayout(
					[HLayout(
						StaticText(win, 110@80 * scale).string_("Search"),
						[searchField = TextField()
							.action_{ arg view;
								model.search(view.value);
								view.string = view.value;
							}
							.maxWidth_(110 * scale),
							align: \left]
					)],
					[listView = ListView(win, 220@200 * scale)
						.maxWidth_(col1Width)
						.background_(Color.white)
						.hiliteColor_(Color.yellow(alpha:0.6))
						.action_({ arg sbs;
							model.glyphName = listView.items[sbs.value];
						})
						.enterKeyAction_({ arg sbs;
							model.glyphName = listView.items[sbs.value];
							model.postGlyphDict
						})
					]
				),
				[VLayout(
					[nameStatic = StaticText()
						.string_("Name: ")
						.minSize_(220@40 * scale)
						.background_(bkgCol)
						, align: \topLeft],
					[codeStatic = StaticText()
						.string_("Code point:")
						.background_(bkgCol)
						.minSize_(220@40 * scale)
						, align: \topLeft],
					[descrStatic = StaticText()
						.string_("Description: ")
						.background_(bkgCol)
						.minSize_(220@80 * scale),
						align: \topLeft],
					[bigGlyph = StaticText().string_("")
						.font_(Font("Bravura", 84 * scale))
						.background_(bkgCol)
						.minSize_(220@240 * scale)
						, align: \topLeft]
				), align: \top]
			)
		).front;
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














