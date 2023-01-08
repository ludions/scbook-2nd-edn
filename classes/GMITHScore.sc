
/*
© 2022 Tom Hall
*/


GMITHCellSelectionModel {
	var <colRange, <cellData, <scoreModel, <mouseSelect, <eventSelect;

	*new {arg scoreModel;
		^super.new.init(scoreModel);
	}

	init {|argModel|
		scoreModel = argModel;
		scoreModel.addDependant(this);
		colRange = [0, 0];
		cellData = Dictionary.new;
		^this
	}

	mouseSelect_ {|mouseCell, modData|
		// change model
		// format("mouseSelect_ %", mouseCell).postln; // testing
		scoreModel.mouseSelectData(mouseCell, modData);
	}

	eventSelect_{|eventSelect|
		scoreModel.eventSelect = eventSelect;
	}

	update {|obj, what, val|
		var keys, dict, changeBool, tmpKeysArr;
		if(val.isEmpty){
			changeBool = true;
		}{
			// ["sel update", obj, what, val].postln;
			if(val.rank == 2){val = val.bubble}; // single event added
			keys = [];
			// cycles over multiple events as needed
			val.do{|j|
				tmpKeysArr = j.collect{|i| i[1]};
				keys = keys ++ tmpKeysArr;
			};
			keys = keys.asSet.asArray.sort;
			if(colRange != [0, 0]){
				// are changed score keys included in system cell range?
				changeBool = (colRange[0]..colRange[1]).includesAny(keys);
			}{
				changeBool = false;// ignore changes if range is empty
			}
		};
		if(changeBool){
			cellData = scoreModel.selectCellColRange(*colRange);
			this.changed(\cellData, cellData);
		};
		^this
	}

	colRange_ {|a, b|
		colRange = [a, b];
		cellData = scoreModel.selectCellColRange(*colRange);
		this.changed(\colRange, colRange);
		this.changed(\cellData, cellData);
		^this
	}

	numCols {
		var range = colRange;
		if(range.notNil){
			^(range[1] - range[0] + 1)
		}{
			^0
		};
	}
}


/////////////////////////////////////////////////

GMITHScore {
	var <eventDict, <cellsDict, <mappingDictFn, <curKey, <>mouseSelect;
	var <removedEvent, <addedEvent, <>eventSelect, <modData, <currAction;

	*new { arg data;
		^super.new.init(data);
	}

	init { arg argdata;
		this.mappingDictFn_();
		eventDict = Dictionary.new;
		cellsDict = Dictionary.new;
		if(argdata.notNil){
			this.addAll(argdata);
		};
		^this
	}

	/*

	~offset = 1;
	f = Dictionary.new; d.keysValuesDo { |key, value| f.add([key[0], key[1] + ~offset] -> value) }; d = f;

	~balanced = d.keys.collect{|i| i[1]}.asArray.minItem
	if(~balanced.notNil){~balanced = ~balanced.neg}

	*/

	// shift left or right
	// creates new dictionary
	moveAll {|moveBy|
		var newEventDict = Dictionary.new;
		if(moveBy.isNil){moveBy = this.firstEventCol.neg};
		eventDict.keysValuesDo { |key, value|
			newEventDict.add([key[0], key[1] + moveBy] -> value)
		};
		this.removeAll(verbose: false);
		this.addDict(newEventDict, verbose: false);
		"moveAll done".postln;
		^this
	}

	addDict {|dict, verbose = true|
		var arr = this.convertDict(dict);
		arr.postln;
		this.addAll(arr, verbose: verbose);
		^this;
	}

	dictRemoveReservedCells {|dict|
		^dict.reject{ |item| item.value[1]==\reserved };
	}

	// this returns the score without the \reserved cells
	scoreDict {
		^this.dictRemoveReservedCells(eventDict);
	}

	// this returns score as Array without \reserved cells
	scoreArr {
		^this.convertDict(eventDict);
	}

	// convert to Array of associations to use addAll
	convertDict {|dict|
		var array, type;
		if(dict.notNil){
			if (dict.notEmpty){
				// strip out reserved cells
				dict = this.dictRemoveReservedCells(dict);
				dict.keysValuesDo({ |key, value|
					array = array.add(key -> value)
				});
			};
		};
		if(array.isNil){array = []};
		^array
	}

	firstEventCol {
		^eventDict.keys.collect{|i| i[1]}.asArray.minItem
	}


	mouseSelectData {|cells, data|
		mouseSelect = cells;
		modData = data;
		//["mouseSelect, modData", mouseSelect, modData].postln;
		case {modData==\shift}{this.mouseMove}
		{modData==\alt}{this.removeMouseCell}
		{modData==\ctrl}{this.add(mouseSelect)}
		{modData==\fun}{"Fn key not implement in GMITH".postln};
		^this
	}

	mappingDictFn_ { |fn|
		mappingDictFn = fn;
		if(mappingDictFn.isNil){
			mappingDictFn = { arg type, single=0;
				var typeDict = Dictionary.with(*[
					nil -> (type: [\square, \start][single]),
					"A" -> (type: [\square, \start][single], \txt: \A),
					"H" -> (type: [\squareDiamond, \startSquareDiamond][single]),
					"P" -> (type: [\square, \start][single], \txt: \Pont)
					//"O" -> (type: [\plain, \start][single])
				]);
				typeDict[type]; // return
			};
		};
		if(cellsDict.notNil){
			this.mapEventsToCells(cellsDict.keys.asArray);
		};
		^this
	}

	// rtn a new dictionary with selected columns range
	selectCellColRange {|a, b|
		var newDict = Dictionary.new;
		cellsDict.keys.do{|i| if(i[1]>=a and: {i[1]<=b}){
			newDict.put(i, cellsDict.at(i));
		};
		};
		// if(newDict.isEmpty){"No cells within selected columns range".warn};
		^newDict
	}

	cellsColRange { |startAt0Bool = true|
		var range, start;
		if(cellsDict.notEmpty){
			range = cellsDict.order(this.orderKeysFn_(1));

			start = if(startAt0Bool){0}{range.first[1]};

			^[start, range.last[1]] // cols range
		}{
			^nil
		};
	}

	cellsNumCols {|startAt0Bool = true|
		var range = this.cellsColRange(startAt0Bool);
		if(range.notNil){
			^(range[1] - range[0] + 1)
		}{
			^0
		};
	}

	cellsNumRows {
		var dict = this.cellsDict.order(this.orderKeysFn_(0));
		if(dict.notNil){
			^(dict.last[0] + 1)
		}{
			^0
		}
		//^cellsDict.order(this.orderKeysFn_(0)).tryPerform(\last).tryPerform(\at, 0);
	}




	// order by row or column
	// [row, col] -> [num, type, dur]
	orderKeysFn_ {arg order = 0;
		var func, ints;
		ints = if(order==0) {[0, 1]} {[order.min(1), 0]};
		func = {|a, b|
			if(a.key[ints[0]] == b.key[ints[0]]) {
				a.key[ints[1]] <= b.key[ints[1]]
			} {
				a.key[ints[0]] <= b.key[ints[0]]
			}
		};
		^func
	}

	// post score events / cells while ignoring \reserved cells
	postDict {arg order = 0, dict, name;
		var keysOrdered, size, orderFn;
		size = dict.size;
		orderFn = this.orderKeysFn_(order);
		// keysOrdered = this.orderKeys(order);
		keysOrdered = dict.order(orderFn);

		("\n///////////////////////////\n(\n~"++name++" = [").postln;
		keysOrdered.do{|i, j|
			// skip reserved cells
			if(dict[i][1] != \reserved){
				format("    % -> %", i, dict[i].asCompileString).post;
				if(j<(size-1)){","}{""}.postln;
			};
		};
		"];\n)\n///////////////////////////\n".postln;
		^this
	}

	postEvents {|order = 0|
		this.postDict(order, eventDict, "events");
		^this
	}

	postCells {|order = 0|
		this.postDict(order, cellsDict, "cells");
		^this
	}

	// (originally from Drawing class )
	// stack array of numbers for multiple events
	prProcessCellText {|text|
		var stack;
		// ["text in:", text].postln;
		if(text.isString.not){
			if(text.isArray){
				text.do({|i|
					if(stack.notNil){stack = stack++"\n"};
					stack = stack++(i.asString)
				});
				text = stack;
			} {
				text = text.asString
			};
		};
		// ["text out:", text].postln;
		^text
	}

	mapEventsToCells { |changedKeys|
		var tmpArr;
		cellsDict = Dictionary.new; // overwrite any existing

		eventDict.keysValuesDo{|key, value|
			tmpArr = this.mapSingleEvent(key -> value);
			if(tmpArr.notNil){
				cellsDict.addAll(tmpArr);
			};
		};
		this.changed(\cellsDict, changedKeys);
		^this
	}

	mapSingleEvent {|assoc|
		var num, type, dur, newVal, newTxt, singleCell;
		var cellsArr, val, key;

		key = assoc.key;
		val = assoc.value;
		type = val[1];
		// ignore reserved cells here,
		// they will be mapped by main event cell
		if(type == \reserved){^nil};
		num = val[0];
		if(type.notNil){type = type.asString.toUpper};
		dur = val[2];
		if(dur.isNil){dur = 1};
		singleCell = if(dur==1){0}{1};
		// values in the mappingDictArr Events
		newVal = mappingDictFn.value(type, singleCell);
		if(num.notNil){
			num = this.prProcessCellText(num);
			// check for extisting txt, add to it as needed
			if(newVal.includesKey(\txt)){
				newTxt = newVal[\txt].asString ++ num;
				newVal.put(\txt, newTxt);
			}{
				newVal.put(\txt, num.asString);
			};
		};
		cellsArr = [assoc.key -> newVal];

		if(dur > 1){
			var tmpType, tmpKey;
			(dur-1).do{|i|
				tmpType = if(i == (dur-2)){\end }{\mid};
				tmpKey = [key[0], key[1] + i+1];
				cellsArr = cellsArr.add(tmpKey -> (type: tmpType));
			};
		};
		^cellsArr;
	}

	isEmptyCell {|key|
		if(key.notNil){
			^eventDict.keys.includes(key).not
		}{
			"Please supply key arg".error;
			^nil
		}
	}

	isReservedCell {|key|
		if(this.isEmptyCell(key).not){
			^eventDict[key][1]==\reserved
		}{
			^nil
		}
	}

	isMultiCellEvent {|key|
		case {this.isEmptyCell(key)}{^nil}
		{this.isReservedCell(key)}{^true};
		if(eventDict[key].size==3){
			^eventDict[key][2] > 1
		}{
			^false
		}
	}

	// find Event key for non empty cell
	// ie takes into account multicell events
	cellEventKey {|key|
		if(key.notNil) {
			case {this.isEmptyCell(key)}{^nil};
			if(this.isMultiCellEvent(key)){
				if(this.isReservedCell(key)){
					^eventDict[key][2]
				}
			};
			^key;
		}
		^nil
	}

	association {|key|
		var cellEventKey;
		if(key.notNil) {
			cellEventKey= this.cellEventKey(key);
			if(cellEventKey.notNil){
				^cellEventKey -> this.cellEvent(cellEventKey)
			}{
				^nil
			}
		}{
			^nil
		}
	}

	cellEvent {|key|
		var eventKey;
		if(key.notNil){
			case {this.isEmptyCell(key)}{^nil};
			eventKey = this.cellEventKey(key);
			^eventDict[eventKey]
		}{
			^nil
		}
	}

	excludesAll { |keysArr, anEventDict|
		var dict;
		dict = anEventDict ?? eventDict;
		keysArr.do{|key|
			if(dict.keys.includes(key)) { ^false }
		};
		^true;
	}

	keyMatches { |keysArr|
		var stack = [];
		keysArr.do{|key|
			if(eventDict.keys.includes(key)) {
				stack = stack.add(key)
			}
		};
		^stack;
	}

	addMouseCell {|events, update = true|
		var assoc;
		if(mouseSelect.notNil){
			if(events.notNil){
				assoc = mouseSelect -> events;
			}{
				assoc = mouseSelect
			};
			this.add(assoc, update);
			//format("Event added at %", mouseSelect).postln;
		}{
			"A mouse has not selected a cell".error
		};
		^this
	}

	removeMouseCell {|update = true|
		if(mouseSelect.notNil){
			this.remove(mouseSelect, update);
			//format("Event removed at %", mouseSelect).postln;
		}{
			"A mouse has not selected a cell".error
		};
		^this
	}


	undo {
		var tmpToAdd;
		case{currAction == \add}{this.remove(addedEvent.key)}
		{currAction == \remove}{this.add(removedEvent)}
		{currAction == \move}{
			tmpToAdd = removedEvent;
			this.remove(addedEvent.key);
			this.add(tmpToAdd);
		}
		{true}{"Nothin to undo".postln;}
		^this
	}


	// [row, col] -> [num, type, dur]
	add {|assoc, update = true, verbose = true|
		var keys;
		// if single box, can enter key alone
		if(assoc.isKindOf(Association).not){
			assoc = Association.new(assoc, []);
		};
		keys = this.prCalcCellRange(assoc);
		if(this.excludesAll(keys)){
			currAction = \add;
			keys.do{|i, j|
				if(j==0){
					eventDict.add(assoc);
					curKey = i; // for undo
				}{
					eventDict.add(i -> [nil, \reserved, assoc.key]);
				};
			};
			addedEvent = assoc;
			if(verbose){
				format("Added at %, event %",
					assoc.key, assoc.value.asCompileString
				).postln;
			};
			if(update){
				this.mapEventsToCells(keys);
			};
			^keys; // needed for addAll
		}{
			this.prCollisionError(assoc, keys);
			^nil // needed for addAll
		};
		^this
	}

	prSafeToAddEvent {|assoc, dict|
		var keys;
		// if single box, can enter key alone
		if(assoc.isKindOf(Association).not){
			assoc = Association.new(assoc, []);
		};
		keys = this.prCalcCellRange(assoc);
		^this.excludesAll(keys, dict)
	}


	mouseMove {
		this.move(eventSelect, mouseSelect);
		^this
	}


	move {|keyFrom, keyTo|
		var successRemove, successAdd, testKeys, testVal, testDict;
		if(keyFrom.notNil and:{keyTo.notNil}){
			// extract key if eventKey is Association
			if(keyFrom.isKindOf(Association)){keyFrom = keyFrom.key};
			keyFrom = this.cellEventKey(keyFrom);
			if(keyFrom !=keyTo){
				successRemove = this.prSafeToRemove(keyFrom);
				if(successRemove){
					// make a dummy dict. in order to test remove and move/add OK
					// in case some overlap btn move positions
					testDict = eventDict.copy;
					testVal = testDict[keyFrom];
					testKeys = this.prCalcCellRange(keyFrom -> testVal);
					testKeys.do{|i| testDict.removeAt(i)};
					successAdd = this.prSafeToAddEvent(keyTo -> testVal, testDict);
					if(successAdd){
						this.remove(keyFrom, verbose: false);
						this.add(keyTo -> removedEvent.value, verbose: false);
						format("Moved event from % to %", keyFrom, keyTo).postln;
						currAction = \move
					}{
						"Can't move event to new position".error
					}
				}{
					"Unable to move an event from here".error
				}
			}{
				"Move from/to keys are the same".error
			}
		}{
			"Two keys required to move a score event".error
		};
		^this

	}


	remove { |eventKey, update = true, verbose = true|
		var eventKeys, assoc, value, eventStartCell;
		// extract key if eventKey is Association
		if(eventKey.isKindOf(Association)){eventKey = eventKey.key};
		eventKey = this.cellEventKey(eventKey);
		value = eventDict[eventKey];
		// check if cell is an event start
		// or a reserved cell ie part of multicell event
		if(this.prSafeToRemove(value)){
			// needs whole event value may indicate multicell
			eventKeys = this.prCalcCellRange(eventKey -> value);
			removedEvent = eventKey -> value;
			currAction = \remove;
			eventKeys.do{|i|
				eventDict.removeAt(i)
			};
			if(verbose){
				format("Removed at %, event %", eventKey, value).postln;
			};
			if(update){
				this.mapEventsToCells(eventKeys);
			};
			^eventKey
		}{
			this.prRemoveError(eventKey, value);
			^nil
		};
	}

	removeAll {|verbose = true|
		eventDict = Dictionary.new;
		cellsDict = Dictionary.new;
		this.changed(\cellsDict, []);
		if(verbose){
			"EventDict and cellsDict cleared".postln;
		};
		^this
	}

	clearAll {|verbose = true|
		this.removeAll(verbose);
		^this
	}

	prRemoveError {|eventKey, value|
		case
		{value.isNil}{
			format("No event at key % to remove", eventKey).error
		}
		{value[1] == \reserved}{
			format("Key % not removed, part of % event", eventKey, value[2]).error
		};
		^this
	}

	prSafeToRemove {|value|
		if(value.notNil){
			^value[1] != \reserved
		}{
			^false
		};
	}


	prCollisionError {arg assoc, cells;
		var matches, reservedCell, collisionInfo;
		matches = this.keyMatches(cells);

		reservedCell = (eventDict[matches[0]][1] == \reserved);
		collisionInfo = if(reservedCell){
			format("reserved by event beginning %", eventDict[matches[0]][2]);
		}{
			eventDict[matches[0]]
		};

		format(
			"Event % not added, collision at cells(s) %",
			assoc,
			matches
		).error;
		format("( Event at key % is % )",
			matches[0],
			collisionInfo
		).postln;
		^this
	}

	// for checking collisions etc
	// [row, col] -> [num, type, dur]
	prCalcCellRange {arg assoc;
		var dur, key, tmpKey, val, keysArr = [];
		key = assoc.key;
		val = assoc.value;
		dur = val[2];
		if(dur.isNil){dur = 1};
		keysArr = keysArr.add(key);
		if(dur>1){
			(dur-1).do{|i|
				tmpKey = [key[0], key[1] + i+1];
				keysArr = keysArr.add(tmpKey);
			};
		};
		^keysArr
	}

	addAll {|arr, verbose = true|
		var key, newKeys = [];
		arr.do{|i|
			key = this.add(i, update: false, verbose: verbose);
			if(key.notNil){
				newKeys = newKeys.add(key);
			};
		};
		if(newKeys.notEmpty){
			this.mapEventsToCells(newKeys);
		};
		^this
	}
}
