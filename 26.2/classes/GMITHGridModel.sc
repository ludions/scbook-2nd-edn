
/*
© 2022 Tom Hall
*/


// This version assumes you know the dimensions you need for your grid
GMITHGridModel {

	var <rows, <margin, <height, <width, <hPattern;
	var <square, <cols, <lines, <patterns, <vPattern;
	var <matrix, halfMarg;

	*new {|size, rows, cols, margin|
		^super.new.init(size, rows, cols, margin);
	}

	init {|argsize, argrows, argcols, argmargin|
		square = argsize;
		rows = argrows;
		cols = argcols;
		margin = argmargin ?? 1;
		halfMarg = (margin / 2).ceil.asInteger;
		height = square * rows;
		width = square * cols;
		hPattern = [[1]];
		vPattern= [[1]];
		patterns = [hPattern, vPattern]; //h, v
		this.calcLines;
		this.calcMatrixRects;
		^this
	}

	setPatterns{|h, v|
		hPattern = h;
		vPattern = v;
		patterns = [hPattern, vPattern];
		^this;
	}

	calcMatrixRects {
		matrix = Array.newClear(cols) ! rows;
		matrix = matrix.collect{|aRow, rowNum|
			aRow.collect{|aCol, colNum|
				Rect((colNum*square)+ halfMarg, (rowNum*square)+ halfMarg, square, square);
			};
		};
		^this
	}

	calcLines {
		var vLines, hLines;
		hLines = (rows+1).collect{|i|
			[Point(0, (i*square)+ halfMarg),
				Point(width+ margin, (i*square)+ halfMarg)
		]};
		vLines = (cols+1).collect{|i|
			[Point((i*square) + halfMarg, 0),
				Point((i*square)+ halfMarg, height + margin)
		]};
		lines = [hLines, vLines];
		^this
	}
}



