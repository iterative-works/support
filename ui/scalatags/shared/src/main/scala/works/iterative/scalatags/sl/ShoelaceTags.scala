package works.iterative.scalatags.sl

import scalatags.Text.all.*

export ShoelaceTags.*
export ShoelaceAttrs.*

object ShoelaceTags:
    val AnimatedImage = tag("sl-animated-image")
    val Alert = tag("sl-alert")
    val Breadcrumb = tag("sl-breadcrumb")
    val Badge = tag("sl-badge")
    val BreadcrumbItem = tag("sl-breadcrumb-item")
    val Avatar = tag("sl-avatar")
    val Button = tag("sl-button")
    val Animation = tag("sl-animation")
    val ButtonGroup = tag("sl-button-group")
    val Card = tag("sl-card")
    val Carousel = tag("sl-carousel")
    val CarouselItem = tag("sl-carousel-item")
    val Checkbox = tag("sl-checkbox")
    val ColorPicker = tag("sl-color-picker")
    val CopyButton = tag("sl-copy-button")
    val Details = tag("sl-details")
    val Dialog = tag("sl-dialog")
    val Divider = tag("sl-divider")
    val Drawer = tag("sl-drawer")
    val Dropdown = tag("sl-dropdown")
    val FormatBytes = tag("sl-format-bytes")
    val FormatDate = tag("sl-format-date")
    val FormatNumber = tag("sl-format-number")
    val Icon = tag("sl-icon")
    val IconButton = tag("sl-icon-button")
    val ImageComparer = tag("sl-image-comparer")
    val Include = tag("sl-include")
    val Input = tag("sl-input")
    val Menu = tag("sl-menu")
    val MenuItem = tag("sl-menu-item")
    val MenuLabel = tag("sl-menu-label")
    val MutationObserver = tag("sl-mutation-observer")
    val Option = tag("sl-option")
    val Popup = tag("sl-popup")
    val ProgressBar = tag("sl-progress-bar")
    val ProgressRing = tag("sl-progress-ring")
    val QrCode = tag("sl-qr-code")
    val Radio = tag("sl-radio")
    val RadioButton = tag("sl-radio-button")
    val RadioGroup = tag("sl-radio-group")
    val Range = tag("sl-range")
    val Rating = tag("sl-rating")
    val RelativeTime = tag("sl-relative-time")
    val ResizeObserver = tag("sl-resize-observer")
    val Select = tag("sl-select")
    val Skeleton = tag("sl-skeleton")
    val Spinner = tag("sl-spinner")
    val SplitPanel = tag("sl-split-panel")
    val Switch = tag("sl-switch")
    val Tab = tag("sl-tab")
    val TabGroup = tag("sl-tab-group")
    val TabPanel = tag("sl-tab-panel")
    val Tag = tag("sl-tag")
    val Textarea = tag("sl-textarea")
    val Tooltip = tag("sl-tooltip")
    val Tree = tag("sl-tree")
    val TreeItem = tag("sl-tree-item")
    val VisuallyHidden = tag("sl-visually-hidden")
end ShoelaceTags

object ShoelaceAttrs:
    // Common attributes
    val variant = attr("variant")
    val size = attr("size")
    val disabled = attr("disabled")
    val loading = attr("loading")
    val open = attr("open")
    val label = attr("label")
    val placement = attr("placement")
    val hoist = attr("hoist")

    // sl-animated-image
    val src = attr("src")
    val alt = attr("alt")
    val play = attr("play")

    // sl-alert and sl-tab
    val closable = attr("closable")
    // sl-alert
    val duration = attr("duration")

    // sl-avatar
    val image = attr("image")
    val initials = attr("initials")
    val shape = attr("shape")

    // sl-button
    val caret = attr("caret")
    val outline = attr("outline")
    val pill = attr("pill")
    val circle = attr("circle")
    val href = attr("href")
    val target = attr("target")
    val rel = attr("rel")
    val download = attr("download")
    val form = attr("form")
    val formaction = attr("formaction")
    val formenctype = attr("formenctype")
    val formmethod = attr("formmethod")
    val formnovalidate = attr("formnovalidate")
    val formtarget = attr("formtarget")

    // sl-animation
    val name = attr("name")
    val delay = attr("delay")
    val direction = attr("direction")
    val easing = attr("easing")
    val endDelay = attr("end-delay")
    val fill = attr("fill")
    val iterations = attr("iterations")
    val iterationStart = attr("iteration-start")
    val playbackRate = attr("playback-rate")

    // sl-carousel
    val loop = attr("loop")
    val navigation = attr("navigation")
    val pagination = attr("pagination")
    val autoplay = attr("autoplay")
    val autoplayInterval = attr("autoplay-interval")
    val slidesPerPage = attr("slides-per-page")
    val slidesPerMove = attr("slides-per-move")
    val orientation = attr("orientation")
    val mouseDragging = attr("mouse-dragging")

    // sl-checkbox
    val indeterminate = attr("indeterminate")
    val helpText = attr("help-text")

    // sl-color-picker
    val format = attr("format")
    val inline = attr("inline")
    val noFormatToggle = attr("no-format-toggle")
    val opacity = attr("opacity")
    val uppercase = attr("uppercase")
    val swatches = attr("swatches")

    // sl-details
    val summary = attr("summary")

    // sl-dialog, sl-drawer
    val noHeader = attr("no-header")

    // sl-dropdown
    val stayOpenOnSelect = attr("stay-open-on-select")
    val distance = attr("distance")
    val skidding = attr("skidding")
    val sync = attr("sync")

    // sl-input, sl-textarea
    val filled = attr("filled")
    val clearable = attr("clearable")
    val passwordToggle = attr("password-toggle")
    val passwordVisible = attr("password-visible")
    val noSpinButtons = attr("no-spin-buttons")
    val pattern = attr("pattern")
    val minlength = attr("minlength")
    val maxlength = attr("maxlength")
    val min = attr("min")
    val max = attr("max")
    val step = attr("step")
    val autocapitalize = attr("autocapitalize")
    val autocorrect = attr("autocorrect")
    val autocomplete = attr("autocomplete")
    val autofocus = attr("autofocus")
    val enterkeyhint = attr("enterkeyhint")
    val spellcheck = attr("spellcheck")
    val inputmode = attr("inputmode")

    // sl-select
    val multiple = attr("multiple")
    val maxOptionsVisible = attr("max-options-visible")

    // sl-switch
    val checked = attr("checked")

    // sl-tab
    val panel = attr("panel")
    val active = attr("active")

    // sl-tab-group
    val activation = attr("activation")
    val noScrollControls = attr("no-scroll-controls")

    // sl-tree
    val selection = attr("selection")

    // sl-tree-item
    val expanded = attr("expanded")
    val selected = attr("selected")
    val `lazy` = attr("lazy")

    // Add any other custom attributes here...
end ShoelaceAttrs
