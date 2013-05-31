package org.zkoss.zss.essential;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;


import org.zkoss.image.AImage;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.IdSpace;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zss.api.CellOperationUtil;
import org.zkoss.zss.api.Range;
import org.zkoss.zss.api.Range.ApplyBorderType;
import org.zkoss.zss.api.model.CellData;
import org.zkoss.zss.api.Range.DeleteShift;
import org.zkoss.zss.api.Range.InsertCopyOrigin;
import org.zkoss.zss.api.Range.InsertShift;
import org.zkoss.zss.api.Ranges;
import org.zkoss.zss.api.SheetOperationUtil;
import org.zkoss.zss.api.model.CellStyle;
import org.zkoss.zss.api.model.CellStyle.BorderType;
import org.zkoss.zss.api.model.Chart;
import org.zkoss.zss.api.model.ChartData;
import org.zkoss.zss.api.model.Font.Boldweight;
import org.zkoss.zss.api.model.Font.Underline;
import org.zkoss.zss.api.model.Sheet;
import org.zkoss.zss.essential.ToolbarCtrl.ClipInfo.Type;
import org.zkoss.zss.essential.util.ClientUtil;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zssex.api.ChartDataUtil;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Menu;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.impl.XulElement;

public class ToolbarCtrl extends SelectorComposer<Component> {
	private static final long serialVersionUID = 1L;

	private static final String ON_INIT_SPREADSHEET = "onInitSpreadsheet";

	String ssid;
	String sspath;
	Spreadsheet nss;

	
	@Wire
	Toolbarbutton paste;
	@Wire
	Toolbarbutton pasteMenu;
	@Wire
	Menupopup pastePopup;
	
	@Wire
	Combobox fontNameBox;
	ListModelList<FontName> fontNameList;
	
	@Wire
	Combobox fontSizeBox;
	ListModelList<Integer> fontSizeList;
	
//	@Wire
//	Colorbox fontColorbox;
//	@Wire
//	Colorbox fillColorbox;
	
	@Wire
	Toolbarbutton fontColorMenu;
	@Wire
	Menupopup fontColorPopup;
	@Wire
	Toolbarbutton fillColorMenu;
	@Wire
	Menupopup fillColorPopup;
	@Wire
	Menu fontColor;
	@Wire
	Menu fillColor;	
	
	
	
	@Wire
	Toolbarbutton alignMenu;
	@Wire
	Menupopup alignPopup;
	
	
	
	@Wire
	Toolbarbutton borderMenu;
	@Wire
	Menupopup borderPopup;
	@Wire
	Menu borderColor;	
	
	
	@Wire
	Toolbarbutton mergeMenu;
	@Wire
	Menupopup mergePopup;
	
	
	@Wire
	Toolbarbutton clearMenu;
	@Wire
	Menupopup clearPopup;
	
	@Wire
	Toolbarbutton insertMenu;
	@Wire
	Menupopup insertPopup;
	
	@Wire
	Toolbarbutton deleteMenu;
	@Wire
	Menupopup deletePopup;
	
	@Wire
	Toolbarbutton sortMenu;
	@Wire
	Menupopup sortPopup;
	
	@Wire
	Toolbarbutton filterMenu;
	@Wire
	Menupopup filterPopup;
	
	
	@Wire
	Toolbarbutton chartMenu;
	@Wire
	Menupopup chartPopup;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Map arg = Executions.getCurrent().getArg();
		ssid = (String) arg.get("spreadsheetId");
		sspath = (String) arg.get("spreadsheetPath");

		comp.addEventListener(ON_INIT_SPREADSHEET, new EventListener<Event>() {
			public void onEvent(Event event) throws Exception {
				getSelf().removeEventListener(ON_INIT_SPREADSHEET, this);
				postInitSpreadsheet();
			}
		});
		Events.postEvent(new Event(ON_INIT_SPREADSHEET, comp));

		String script = "focusSS(this,'" + ssid + "')";
		for (Component c : Selectors.find(comp, "toolbarbutton")) {
			((XulElement) c).setWidgetListener("onClick", script);
		}
		for (Component c : Selectors.find(comp, "combobox")) {
			((XulElement) c).setWidgetListener("onClick", script);
			((XulElement) c).setWidgetListener("onSelect", script);
		}
		
		
	}

	public void postInitSpreadsheet() {
		Component comp = getSelf();

		Spreadsheet ss = (Spreadsheet) comp.getFellowIfAny(ssid);
		if (ss == null && sspath != null) {
			IdSpace o = comp.getSpaceOwner();
			if (o != null) {
				ss = (Spreadsheet) Path.getComponent(o, sspath);
			}
		}
		if (ss == null && sspath != null) {
			ss = (Spreadsheet) Path.getComponent(sspath);
		}

		if (ss == null) {
			throw new IllegalArgumentException(
					"spreadsheet component not found with id " + ssid);
		}

		nss = ss;
		ssid = null;

		// initial font family
		fontNameList = new ListModelList<FontName>();
		fontNameList.add(new FontName("Arial", "Arial",
				"arial"));
		fontNameList.add(new FontName("Courier New", "Courier New",
				"courier-new"));
		fontNameList.add(new FontName("Times New Roman", "Times New Roman",
				"times-new-roman"));
		fontNameList.add(new FontName("MS Sans Serif", "MS Sans Serif",
				"ms-sans-serif"));

		fontNameBox.setModel(fontNameList);
		
		
		fontSizeList = new ListModelList<Integer>();
		fontSizeList.add(12);
		fontSizeList.add(14);
		fontSizeList.add(18);
		fontSizeList.add(24);
		fontSizeList.add(36);
		fontSizeList.add(48);

		fontSizeBox.setModel(fontSizeList);
		
		clearClipboard();
	}

	ClipInfo clipinfo;

	static class ClipInfo {
		enum Type {
			COPY, CUT
		}

		Type type;
		Sheet sheet;
		Rect sel;

		public ClipInfo(Sheet sheet, Rect sel, Type type) {
			this.type = type;
			this.sheet = sheet;// TODO should I keep sheet instance?consider it
								// again.
			this.sel = sel;
		}
	}

	private void clearClipboard() {
		clipinfo = null;
		nss.setHighlight(null);
		pasteMenu.setDisabled(true);
		paste.setDisabled(true);
	}

	private void setClipboard(Sheet sheet, Rect sel, ClipInfo.Type type) {
		clipinfo = new ClipInfo(sheet, sel, type);
		nss.setHighlight(sel);
	}
	
	enum PasteType{
		ALL,
		VALUE,
		FORMULA,
		ALL_NO_BORDER,
		TRANSPORT
	}

	@Listen("onClick=#pasteMenu")
	public void doPasteMenu(){
		pastePopup.open(pasteMenu);
	}

	@Listen("onClick=#paste")
	public void doPaste() {
		doPaste0(PasteType.ALL);
	}
	@Listen("onClick=#pasteValue")
	public void doPasteValue() {
		doPaste0(PasteType.VALUE);
	}
	@Listen("onClick=#pasteFormula")
	public void doPasteFormula() {
		doPaste0(PasteType.FORMULA);
	}
	@Listen("onClick=#pasteAllNOBorder")
	public void doAllNOBorder() {
		doPaste0(PasteType.ALL_NO_BORDER);
	}
	@Listen("onClick=#pasteTranspose")
	public void doPasteTransport() {
		doPaste0(PasteType.TRANSPORT);
	}
	
	
	private void doPaste0(PasteType type) {
		if (clipinfo == null)
			return;

		Rect sel = getSafeSelection();
		if (sel == null)
			return;

		// check if in the same book only
		if (nss.getBook().getSheetIndex(clipinfo.sheet) < 0) {
			clearClipboard();
			return;
		}

		Range src = Ranges.range(clipinfo.sheet, clipinfo.sel.getTop(),
				clipinfo.sel.getLeft(), clipinfo.sel.getBottom(),
				clipinfo.sel.getRight());

		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());

		
		
		if (dest.isProtected()) {
			// show protected message.
			ClientUtil.showWarn("Cann't paste to a protected sheet/area");
			return;
		} else if (clipinfo.type == Type.CUT && src.isProtected()) {
			ClientUtil.showWarn("Cann't cut from a protected sheet/area");
			return;
		}
		
		
		
		if (clipinfo.type == Type.CUT) {
			CellOperationUtil.cut(src,dest);
			clearClipboard();
		}else{
			switch(type){
			case ALL:
				CellOperationUtil.paste(src,dest);
				break;
			case VALUE:
				CellOperationUtil.pasteValue(src, dest);
				break;
			case FORMULA:
				CellOperationUtil.pasteFormula(src, dest);
				break;
			case ALL_NO_BORDER:
				CellOperationUtil.pasteAllExceptBorder(src, dest);
				break;
			case TRANSPORT:
				CellOperationUtil.pasteTranspose(src, dest);
				break;
			}
		}

	}
	
	private void showProtectionMessage(){
		ClientUtil.showWarn("Cann't modify a protected sheet/area");
	}

	@Listen("onClick=#copy")
	public void doCopy() {
		Rect sel = getSafeSelection();
		setClipboard(nss.getSelectedSheet(), sel, ClipInfo.Type.COPY);
		pasteMenu.setDisabled(false);
		paste.setDisabled(false);
	}

	@Listen("onClick=#cut")
	public void doCut() {
		Rect sel = getSafeSelection();
		setClipboard(nss.getSelectedSheet(), sel, ClipInfo.Type.CUT);
		//TODO should disable some past-special toolbar button
		pasteMenu.setDisabled(true);
		paste.setDisabled(false);
	}

	static public class FontName implements Serializable {
		private static final long serialVersionUID = 1L;
		String displayStyle;
		String displayName;
		String fontName;

		public FontName(String fontName, String displayName,
				String displayStyle) {
			this.fontName = fontName;
			this.displayName = displayName;
			this.displayStyle = displayStyle;
		}

		public String getDisplayStyle() {
			return displayStyle;
		}
		public String getDisplayName() {
			return displayName;
		}

		public String getFontName() {
			return fontName;
		}
	}

	@Listen("onSelect=#fontNameBox")
	public void doFontName() {
		FontName font = fontNameList.getSelection().iterator().next();
		if (font == null) {
			return;
		}
		String fontName = font.getFontName();
		//
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		
		
		CellOperationUtil.applyFontName(dest, fontName);
	}
	
	@Listen("onSelect=#fontSizeBox")
	public void doFontSize() {
		Integer size = fontSizeList.getSelection().iterator().next();
		if (size == null) {
			return;
		}
		
		//
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyFontSize(dest,size.shortValue());
	}
	
	@Listen("onClick=#fontBold")
	public void doFontBold() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		Range first = dest.toCellRange(0, 0);
		
		//toggle and apply bold of first cell to dest
		Boldweight bw = first.getCellStyle().getFont().getBoldweight();
		if(Boldweight.BOLD.equals(bw)){
			bw = Boldweight.NORMAL;
		}else{
			bw = Boldweight.BOLD;
		}
		
		CellOperationUtil.applyFontBoldweight(dest, bw);	
	}

	
	@Listen("onClick=#fontItalic")
	public void doFontItalic() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		Range first = dest.toCellRange(0, 0);
		
		//toggle and apply bold of first cell to dest
		boolean italic = !first.getCellStyle().getFont().isItalic();
		CellOperationUtil.applyFontItalic(dest, italic);	
	}
	
	@Listen("onClick=#fontStrike")
	public void doFontStrike() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		Range first = dest.toCellRange(0, 0);
		
		//toggle and apply bold of first cell to dest
		boolean strikeout = !first.getCellStyle().getFont().isStrikeout();
		CellOperationUtil.applyFontStrikeout(dest, strikeout);
	}
	
	
	@Listen("onClick=#fontUnderline")
	public void doFontUnderline() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		Range first = dest.toCellRange(0, 0);
		
		//toggle and apply bold of first cell to dest
		Underline underline = first.getCellStyle().getFont().getUnderline();
		if(Underline.NONE.equals(underline)){
			underline = Underline.SINGLE;
		}else{
			underline = Underline.NONE;
		}
		
		CellOperationUtil.applyFontUnderline(dest, underline);	
	}
	
	
	private Rect getSafeSelection(){
		Rect sel = nss.getSelection();
		//TODO re-format sel before zss support well rows,colums selection handling
		int r = sel.getRight();
		int b = sel.getBottom();
		sel = new Rect(sel.getLeft(), sel.getTop(),
				(r <= nss.getMaxVisibleColumns()) ? r : nss.getMaxVisibleColumns(),
				(b <= nss.getMaxVisibleRows()) ? b : nss.getMaxVisibleRows());

		return sel;
	}
	
	private Rect getSelection(){
		return nss.getSelection();
	}
	
	//Color
	@Listen("onClick=#fontColorMenu")
	public void doFontColorMenu(){
		fontColorPopup.open(fontColorMenu);
	}
	@Listen("onClick=#fillColorMenu")
	public void doFillColorMenu(){
		fillColorPopup.open(fillColorMenu);
	}
	
	@Listen("onChange=#fontColor")
	public void doFontColor(){
		String htmlColor = fontColor.getContent(); //'#HEX-RGB'
		if(htmlColor==null){
			return;
		}
		htmlColor = htmlColor.substring(htmlColor.lastIndexOf("#"));

		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyFontColor(dest, htmlColor);		
	}
	
	@Listen("onChange=#fillColor")
	public void doFillColor(){
		String htmlColor = fillColor.getContent(); //'#HEX-RGB'
		if(htmlColor==null){
			return;
		}
		htmlColor = htmlColor.substring(htmlColor.lastIndexOf("#"));

		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyBackgroundColor(dest, htmlColor);		
	}
	
	//Align
	@Listen("onClick=#alignMenu")
	public void doAlignMenu(){
		alignPopup.open(alignMenu);
	}

	@Listen("onClick=#alignTop")
	public void doAlignTop() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyVerticalAlignment(dest,CellStyle.VerticalAlignment.TOP);
	}
	@Listen("onClick=#alignMiddle")
	public void doAlignMiddle() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyVerticalAlignment(dest,CellStyle.VerticalAlignment.CENTER);
	}
	@Listen("onClick=#alignBottom")
	public void doAlignBottom() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyVerticalAlignment(dest,CellStyle.VerticalAlignment.BOTTOM);
	}
	
	
	@Listen("onClick=#alignLeft")
	public void doAlignLeft() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyAlignment(dest,CellStyle.Alignment.LEFT);
	}
	@Listen("onClick=#alignCenter")
	public void doAlignCenter() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyAlignment(dest,CellStyle.Alignment.CENTER);
	}
	@Listen("onClick=#alignRight")
	public void doAlignRight() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyAlignment(dest,CellStyle.Alignment.RIGHT);
	}
	
	
	//Border
	@Listen("onClick=#borderMenu")
	public void doBorderMenu(){
		borderPopup.open(borderMenu);
	}
	@Listen("onClick=#borderAll")
	public void onBorderAll(){
		doBorder0(ApplyBorderType.FULL,BorderType.MEDIUM);
	}
	@Listen("onClick=#borderNo")
	public void onBorderNo(){
		doBorder0(ApplyBorderType.FULL,BorderType.NONE);
	}
	
	@Listen("onClick=#borderBottom")
	public void onBorderBottom(){
		doBorder0(ApplyBorderType.EDGE_BOTTOM,BorderType.MEDIUM);
	}
	
	@Listen("onClick=#borderTop")
	public void onBorderTop(){
		doBorder0(ApplyBorderType.EDGE_TOP,BorderType.MEDIUM);
	}
	
	@Listen("onClick=#borderLeft")
	public void onBorderLeft(){
		doBorder0(ApplyBorderType.EDGE_LEFT,BorderType.MEDIUM);
	}
	
	@Listen("onClick=#borderRight")
	public void onBorderRight(){
		doBorder0(ApplyBorderType.EDGE_RIGHT,BorderType.MEDIUM);
	}
	
	@Listen("onClick=#borderOutside")
	public void onBorderOutside(){
		doBorder0(ApplyBorderType.OUTLINE,BorderType.MEDIUM);
	}
	
	@Listen("onClick=#borderInside")
	public void onBorderInside(){
		doBorder0(ApplyBorderType.INSIDE,BorderType.MEDIUM);
	}
	
	@Listen("onClick=#borderInsideHorizontal")
	public void onBorderInsideHor(){
		doBorder0(ApplyBorderType.INSIDE_HORIZONTAL,BorderType.MEDIUM);
	}
	
	@Listen("onClick=#borderInsideVertical")
	public void onBorderInsideVer(){
		doBorder0(ApplyBorderType.INSIDE_VERTICAL,BorderType.MEDIUM);
	}
	
	
	
	private void doBorder0(ApplyBorderType type,BorderType borderTYpe){
		String htmlColor = borderColor.getContent(); //'#HEX-RGB'
		if(htmlColor==null){
			return;
		}
		htmlColor = htmlColor.substring(htmlColor.lastIndexOf("#"));
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.applyBorder(dest,type, borderTYpe, htmlColor);
	}
	
	// Wrap
	@Listen("onClick=#wrapTextMenu")
	public void doWrapText() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		Range first = dest.toCellRange(0, 0);
		
		//toggle and apply 
		boolean wrapped = first.getCellStyle().isWrapText();
		
		wrapped = !wrapped;
		
		CellOperationUtil.applyWrapText(dest, wrapped);	
	}
	
	// Merge
	@Listen("onClick=#mergeMenu")
	public void doMergeMenu() {
		mergePopup.open(mergeMenu);
	}

	@Listen("onClick=#mergeCenter")
	public void onMergeCenter() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.toggleMergeCenter(dest);
	}

	@Listen("onClick=#mergeAcross")
	public void onMergeAcross() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.merge(dest, true);
	}

	@Listen("onClick=#mergeAll")
	public void onMergeAll() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.merge(dest, false);
	}

	@Listen("onClick=#unMerge")
	public void onUnMerge() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.unMerge(dest);
	}
	
	
	// Clear
	@Listen("onClick=#clearMenu")
	public void doClearMenu() {
		clearPopup.open(clearMenu);
	}
	
	@Listen("onClick=#clearContents")
	public void onClearContents() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.clearContents(dest);
	}
	
	@Listen("onClick=#clearStyles")
	public void onClearStyles() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.clearStyles(dest);
	}
	
	@Listen("onClick=#clearAll")
	public void onClearAll() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.clearAll(dest);
	}
	
	// Insert
	@Listen("onClick=#insertMenu")
	public void doInsertMenu() {
		insertPopup.open(insertMenu);
	}

	@Listen("onClick=#insertCellRight")
	public void onInsertCellRight() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		
		CellOperationUtil.insert(dest,InsertShift.RIGHT, InsertCopyOrigin.FORMAT_RIGHT_BELOW);
		
		clearClipboard();
	}
	
	@Listen("onClick=#insertCellDown")
	public void onInsertCellDown() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		
		CellOperationUtil.insert(dest,InsertShift.DOWN, InsertCopyOrigin.FORMAT_LEFT_ABOVE);
		
		clearClipboard();
	}
	
	@Listen("onClick=#insertRows")
	public void onInsertRows() {
		Rect sel = getSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		dest = dest.toRowRange();//get row with whole column
//		System.out.println(">>>>rows "+dest.getRow()+","+dest.getLastRow()+","+dest.getColumn()+","+dest.getLastColumn());
		
		if(dest.isWholeColumn()){
			ClientUtil.showWarn("Cann't insert more row");
			return;
		}
		CellOperationUtil.insert(dest,InsertShift.DOWN, InsertCopyOrigin.FORMAT_LEFT_ABOVE);
		
		clearClipboard();
	}
	
	@Listen("onClick=#insertColumns")
	public void onInsertColumns() {
		Rect sel = getSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		dest = dest.toColumnRange();//get column with whole rows
//		System.out.println(">>>>columns "+dest.getRow()+","+dest.getLastRow()+","+dest.getColumn()+","+dest.getLastColumn());
		if(dest.isWholeRow()){
			ClientUtil.showWarn("Cann't insert more column");
			return;
		}
		CellOperationUtil.insert(dest,InsertShift.RIGHT, InsertCopyOrigin.FORMAT_RIGHT_BELOW);
		
		clearClipboard();
	}
	
	// Delete
	@Listen("onClick=#deleteMenu")
	public void doDeleteMenu() {
		deletePopup.open(deleteMenu);
	}
	@Listen("onClick=#deleteCellLeft")
	public void onDeleteCellLeft() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.delete(dest,DeleteShift.LEFT);
		clearClipboard();
		
	}
	
	@Listen("onClick=#deleteCellUp")
	public void onDeleteCellUp() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		CellOperationUtil.delete(dest,DeleteShift.UP);
		clearClipboard();
	}
	
	@Listen("onClick=#deleteRows")
	public void onDeleteRows() {
		Rect sel = getSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		dest = dest.toRowRange();
		if(dest.isWholeColumn()){
			ClientUtil.showWarn("Cann't delete all rows");
			return;
		}
		CellOperationUtil.delete(dest,DeleteShift.UP);
		clearClipboard();
	}
	
	@Listen("onClick=#deleteColumns")
	public void onDeleteColumns() {
		Rect sel = getSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if(dest.isProtected()){
			showProtectionMessage();
			return;
		}
		dest = dest.toColumnRange();
		if(dest.isWholeRow()){
			ClientUtil.showWarn("Cann't delete all columns");
			return;
		}
		CellOperationUtil.delete(dest,DeleteShift.LEFT);
		clearClipboard();
	}
	
	
	//sort
	@Listen("onClick=#sortMenu")
	public void doSortMenu() {
		sortPopup.open(sortMenu);
	}

	@Listen("onClick=#sortAscending")
	public void onSortAscending() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if (dest.isProtected()) {
			showProtectionMessage();
			return;
		}
		CellOperationUtil.sort(dest,false);
		clearClipboard();
	}
	
	@Listen("onClick=#sortDescending")
	public void onSortDescending() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if (dest.isProtected()) {
			showProtectionMessage();
			return;
		}
		CellOperationUtil.sort(dest,true);
		clearClipboard();
	}
	
	
	//filter
	@Listen("onClick=#filterMenu")
	public void doFilterMenu() {
		filterPopup.open(filterMenu);
	}
	
	@Listen("onClick=#toggleFilter")
	public void onToggleFilter() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if (dest.isProtected()) {
			showProtectionMessage();
			return;
		}
		
		SheetOperationUtil.toggleAutoFilter(dest);
		clearClipboard();
	}
	
	@Listen("onClick=#resetFilter")
	public void onResetFilter() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if (dest.isProtected()) {
			showProtectionMessage();
			return;
		}
		
		SheetOperationUtil.resetAutoFilter(dest);
		clearClipboard();
	}
	
	@Listen("onClick=#applyFilter")
	public void onApplyFilter() {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if (dest.isProtected()) {
			showProtectionMessage();
			return;
		}
		
		SheetOperationUtil.applyAutoFilter(dest);
		clearClipboard();
	}
	
	//image
	@Listen("onUpload=#addImage")
	public void onAddImage(UploadEvent evt) {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		//TODO should check protection before upload, but how ? if we didn't use Filulpoad.get()
		if (dest.isProtected()) {
			showProtectionMessage();
			return;
		}
		
		Media media = evt.getMedia();
		if(media==null){
			ClientUtil.showWarn("Can't get the uploaded file");
			return;
		}
		
		if(!(media instanceof AImage) || SheetOperationUtil.getPictureFormat((AImage)media)==null){
			ClientUtil.showWarn("Can't support the uploaded file");
			return;
		}
		SheetOperationUtil.addPicture(dest,(AImage)media);
		clearClipboard();
	}


	
	//chart
	//filter
	@Listen("onClick=#chartMenu")
	public void doChartMenu() {
		chartPopup.open(chartMenu);
	}

	@Listen("onClick=#addColumnChart")
	public void onAddColumnChart() {
		addChart(Chart.Type.COLUMN, Chart.Grouping.STANDARD, Chart.LegendPosition.RIGHT);
		
		clearClipboard();
	}
	
	@Listen("onClick=#addLineChart")
	public void onAddLineChart() {
		addChart(Chart.Type.LINE, Chart.Grouping.STANDARD, Chart.LegendPosition.RIGHT);
		
		clearClipboard();
	}
	
	@Listen("onClick=#addPieChart")
	public void onAddPieChart() {
		addChart(Chart.Type.PIE, Chart.Grouping.STANDARD, Chart.LegendPosition.RIGHT);
		
		clearClipboard();
	}
	
	@Listen("onClick=#addBarChart")
	public void onAddBarChart() {
		addChart(Chart.Type.BAR, Chart.Grouping.STANDARD, Chart.LegendPosition.RIGHT);
		
		clearClipboard();
	}
	
	@Listen("onClick=#addAreaChart")
	public void onAddAreaChart() {
		addChart(Chart.Type.AREA, Chart.Grouping.STANDARD, Chart.LegendPosition.RIGHT);
		
		clearClipboard();
	}
	
	@Listen("onClick=#addScatterChart")
	public void onAddScatterChart() {
		addChart(Chart.Type.SCATTER, Chart.Grouping.STANDARD, Chart.LegendPosition.RIGHT);
		
		clearClipboard();
	}
	
	@Listen("onClick=#addDoughnutChart")
	public void onAddDoughnutChart() {
		addChart(Chart.Type.DOUGHNUT, Chart.Grouping.STANDARD, Chart.LegendPosition.RIGHT);
		
		clearClipboard();
	}

	private void addChart(Chart.Type type, Chart.Grouping grouping, Chart.LegendPosition pos) {
		Rect sel = getSafeSelection();
		Range dest = Ranges.range(nss.getSelectedSheet(), sel.getTop(),
				sel.getLeft(), sel.getBottom(), sel.getRight());
		if (dest.isProtected()) {
			showProtectionMessage();
			return;
		}
		//set anchor to selection area
		ChartData data = ChartDataUtil.getChartData(nss.getSelectedSheet(),sel, type);
		
		SheetOperationUtil.addChart(dest,data,type,grouping,pos);
		clearClipboard();
	}
	
	
	@Listen("onClick=#test")
	public void onTest() {
		Sheet sheet = nss.getBook().getSheet("CellValue");

		for (int c = 0; c < 4; c++) {
			for (int r = 0; r < 5; r++) {
				Range range = Ranges.range(sheet, r, c);
				CellData cvh = range.getCellData();
				System.out.print("Cell[" + r + "," + c + "]");
				System.out.print(",\tType:" + cvh.getType());
				System.out.print(",\tValue:" + cvh.getValue());
				System.out.print(",\tClass:" + (cvh.getValue()==null?null:cvh.getValue().getClass()));
				System.out.print(",\tEditText:" + cvh.getEditText());
				System.out.println(",\tFormatText:" + cvh.getFormatText());
			}
		}
		Ranges.range(sheet, 0, 0).getCellData().setValue(null);
		Ranges.range(sheet, 1, 0).getCellData().setEditText("=SUM(B1:B3)");
		Ranges.range(sheet, 2, 0).getCellData().setValue("ABC");
		Ranges.range(sheet, 3, 0).getCellData().setValue(Boolean.TRUE);
		Ranges.range(sheet, 4, 0).getCellData().setValue(33.333);
		Ranges.range(sheet, 5, 0).getCellData().setValue(new Date());
	}
}
