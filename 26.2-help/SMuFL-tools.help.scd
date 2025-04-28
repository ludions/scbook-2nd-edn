
/*
SMuFLtools help

© 2022–2025 Tom Hall
ludions.com


Dependencies:
- MITHUnicode
- a SMuFL font, e.g. Bravura (default) https://github.com/steinbergmedia/bravura/

SMuFLtools is a convenience class for searching SMuFL fonts. See:

*/

"https://w3c.github.io/smufl/latest/".openOS

// A SMuFl font needs to be installed for the class to function.

// check if default font Bravura is installed
Font.availableFonts.collect({|i| (i == "Bravura")}).includes(true)

// A small demo subset of SMuFL font metadata in JSON form is provided with the class.
// Download of the complete SMuFL font metadata, under 'Releases', is avaialable here:

"https://github.com/w3c/smufl".openOS

// The class instance has a 'filePath' argument

// If filePath is nil, demo JSON data is loaded
h = SMuFLtools.new

// or, eg.: h = SMuFLtools.new("/path/to/my/JSON/file/glyphnames.json")

h.filePath // for info

// or select a different JSON data file
h.pathFinder // GUI picker

// GUI may be useful, is optional
h.gui

// select a glyph directly by its full name
h.selectGlyph(\noteheadWhole)
h.selectGlyph(\gClef)

// search for a glyph
h.search("clef")

h.clear // clear search

// partial glyph search terms OK
h.search("note")
h.search("acc")

// show all results in Post Window
// by default all keys if no search performed
h.printSearchResults // pretty print, gives index of result also

h.searchKeys // Array

// navigate to the second search result via code
h.glyphIndex_(1) // to update glyph properties

// selected glyph properties
h.glyphName
h.glyphCodepoint
h.glyphDesc

// Dictionary of all properties
h.singleGlyphDict



















