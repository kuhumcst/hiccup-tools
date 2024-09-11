Hiccup tools
============
This library contains various functions for manipulating Hiccup data. The crown jewel of this effort is the function `split-hiccup` which can structurally split complex Hiccup trees into multiple tree structures.

My own use case was splitting an XML document into multiple pages at certain points so that it could be properly paginated within an HTML-based carousel widget without losing any of the structural information present in the original document.

## Function overview
This section will likely expand in future once I develop new functionality needed for other projects.

### dk.cst.hiccup-tools.hiccup

#### `cut` - cut/paste nodes
Remove all nodes matching `pred` from some Hiccup data. The matching nodes are preserved as metadata in the returned Hiccup data so that they may be used for other purposes. This function is meant to replicate the familiar cut/paste functionality.

#### `search` - find nodes matching predicates
Return all nodes matching a list of predicates in a single pass through the Hiccup document. The results are returned as lists keyed to the keys supplied in the input map from key -> predicate.

#### `split` - perform structural splits
Hiccup (and therefore HTML/XML) is fundamentally a tree structure, however it is best to think of any individual point in this tree as a stack of elements applied to some plain text data. These elements represent the combined structural and/or stylistic attributes of the data at that point.

If we want to paginate a structured document like this we can't just call `str/split`; we need to be able to split the document _without_ losing this stack of stylistic and structural information. This requires parsing the data structure, determining where to split (based on a predicate), and splitting it in a way that preserves the stack on either side of the split.

Using the `split` function in this library, supply a `pred` to split with and some `hiccup` data. The function returns another Hiccup data structure where the root node is preserved, but the parts of the document between the split points are now sibling-level. I have used this functionality to paginate highly diverse [TEI documents](https://tei-c.org/) for [interactive display](https://github.com/kuhumcst/clarin-tei) in a browser (here's an [example](https://alf.hum.ku.dk/clarin/tei/reader/aalbor_1633-A_CTB.xml)).

### dk.cst.hiccup-tools.zip

#### `top-level` - get to the top
This is a zipper-specific navigation function that provides the top-most loc in the zipper that _isn't_ the root.

#### `skip-ahead` - skip every node until
This is a zipper-specific navigation function that provides the ability to skip ahead in a zipper until the loc containing a specific node has been found.

#### `split-node` and `split-tree`
These are the functions that facilitate the structural splits obtained with `dk.cst.hiccup-tools.hiccup/split`.

### dk.cst.hiccup-tools.match
Functions for creating predicates that match against Hiccup nodes. These can be used with the functions in `dk.cst.hiccup-tools.hiccup`.

### dk.cst.hiccup-tools.elem
Functions for doing operations on Hiccup vectors; no dependency on `clojure.zip` or any other namespace.

## Background
I like to work with [Hiccup](https://github.com/weavejester/hiccup) data in Clojure projects. Hiccup has long since passed from being merely a popular library to being _the_ standard way of representing HTML/XML/SGML in Clojure. Many Clojure libraries either produce Hiccup or take it as input, e.g. Reagent. For this reason, it is quite convenient to be able to operate directly on Hiccup in both the frontend and backend.

One of the most flexible ways to do this is to treat the Hiccup tree as a [zipper](https://clojuredocs.org/clojure.zip/zipper) using `clojure.zip`. I have some [prior experience](https://github.com/kuhumcst/cuphic) attempting an entire DSL for both searching through and manipulating Hiccup data, but it was perhaps a bit too big of a mouthful for me (I don't have enough knowledge & resources to fully develop an extensive DSL like that) so I am not actively maintaining it now.

However, I still prefer Hiccup and associated zipper-based functions to many other ways of manipulating XML and HTML. This library is the place where I collect this kind of functionality.

Simon Gray,
Centre for Language Technology (University of Copenhagen)
