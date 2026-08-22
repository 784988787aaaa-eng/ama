package com.example.data.serialization.excel

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Low-level OpenXML (.xlsx) SpreadsheetML builder.
 * Generates valid, compliant Office Open XML packages with custom styles,
 * fonts, fills, borders, number formats, and native RTL worksheet layouts.
 */
object XlsxOpenXmlBuilder {

    class SheetColumn(val min: Int, val max: Int, val width: Double)
    class MergeRange(val ref: String)

    class Cell(val col: Int, val value: Any?, val styleId: Int) {
        fun toXml(row: Int): String {
            val ref = getCellRef(col, row)
            if (value == null) return "<c r=\"$ref\" s=\"$styleId\"/>"
            return when (value) {
                is Number -> "<c r=\"$ref\" s=\"$styleId\"><v>${value}</v></c>"
                is Boolean -> "<c r=\"$ref\" s=\"$styleId\" t=\"b\"><v>${if (value) 1 else 0}</v></c>"
                else -> {
                    val esc = value.toString().xmlEscape()
                    "<c r=\"$ref\" s=\"$styleId\" t=\"inlineStr\"><is><t>$esc</t></is></c>"
                }
            }
        }
    }

    class Row(val r: Int, val ht: Int = 24) {
        val cells = mutableListOf<Cell>()

        fun cell(col: Int, value: Any?, styleId: Int) {
            cells.add(Cell(col, value, styleId))
        }

        fun toXml(): String {
            val sb = StringBuilder()
            sb.append("<row r=\"$r\" ht=\"$ht\" customHeight=\"1\">")
            val sortedCells = cells.sortedBy { it.col }
            for (c in sortedCells) {
                sb.append(c.toXml(r))
            }
            sb.append("</row>")
            return sb.toString()
        }
    }

    fun getCellRef(colIndex: Int, rowIndex: Int): String {
        var temp = colIndex
        val colName = StringBuilder()
        while (temp >= 0) {
            colName.insert(0, ('A'.code + temp % 26).toChar())
            temp = temp / 26 - 1
        }
        return "$colName$rowIndex"
    }

    fun String.xmlEscape(): String {
        return this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0.00"/>
  </numFmts>
  <fonts count="6">
    <!-- 0: Regular -->
    <font><sz val="10.5"/><name val="Segoe UI"/><color rgb="FF1E293B"/></font>
    <!-- 1: Bold White for Header -->
    <font><b/><sz val="10.5"/><name val="Segoe UI"/><color rgb="FFFFFFFF"/></font>
    <!-- 2: Bold Dark -->
    <font><b/><sz val="10.5"/><name val="Segoe UI"/><color rgb="FF0F172A"/></font>
    <!-- 3: Bold Red -->
    <font><b/><sz val="10"/><name val="Segoe UI"/><color rgb="FFB91C1C"/></font>
    <!-- 4: Bold Green -->
    <font><b/><sz val="10"/><name val="Segoe UI"/><color rgb="FF156534"/></font>
    <!-- 5: Huge Header Bold -->
    <font><b/><sz val="15"/><name val="Segoe UI"/><color rgb="FF0F4C43"/></font>
  </fonts>
  <fills count="9">
    <!-- 0: None -->
    <fill><patternFill patternType="none"/></fill>
    <!-- 1: Gray125 -->
    <fill><patternFill patternType="gray125"/></fill>
    <!-- 2: Slate Header -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF2C3E50"/><bgColor rgb="FF2C3E50"/></patternFill></fill>
    <!-- 3: Soft Green -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF0FDF4"/><bgColor rgb="FFF0FDF4"/></patternFill></fill>
    <!-- 4: Soft Red -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEF2F2"/><bgColor rgb="FFFEF2F2"/></patternFill></fill>
    <!-- 5: Light Gray / Summary Card -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/><bgColor rgb="FFF8FAFC"/></patternFill></fill>
    <!-- 6: Deep Accent / Brand Green -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF0F4C43"/><bgColor rgb="FF0F4C43"/></patternFill></fill>
    <!-- 7: Soft Blue -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/><bgColor rgb="FFEFF6FF"/></patternFill></fill>
    <!-- 8: Soft Gold -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEF3C7"/><bgColor rgb="FFFEF3C7"/></patternFill></fill>
  </fills>
  <borders count="3">
    <!-- 0: None -->
    <border><left/><right/><top/><bottom/></border>
    <!-- 1: Thin Gray -->
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
    </border>
    <!-- 2: Double Bottom -->
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FF2C3E50"/></top>
      <bottom style="double"><color rgb="FF2C3E50"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="20">
    <!-- 0: Normal regular text -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyFont="1" applyBorder="1">
      <alignment vertical="center" wrapText="1"/>
    </xf>
    <!-- 1: Header row Slate -->
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 2: Debit Cell -->
    <xf numFmtId="164" fontId="3" fillId="4" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 3: Credit Cell -->
    <xf numFmtId="164" fontId="4" fillId="3" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 4: Balance Cell -->
    <xf numFmtId="164" fontId="2" fillId="0" borderId="1" applyFont="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 5: Label Bold -->
    <xf numFmtId="0" fontId="2" fillId="0" borderId="1" applyFont="1" applyBorder="1">
      <alignment horizontal="right" vertical="center" wrapText="1"/>
    </xf>
    <!-- 6: Normal Centered -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyFont="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 7: Summary Card Label -->
    <xf numFmtId="0" fontId="2" fillId="5" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 8: Summary Card Amount Red -->
    <xf numFmtId="164" fontId="3" fillId="5" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 9: Summary Card Amount Green -->
    <xf numFmtId="164" fontId="4" fillId="5" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 10: Summary Card Amount Normal -->
    <xf numFmtId="164" fontId="2" fillId="5" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 11: Totals Row Label -->
    <xf numFmtId="0" fontId="2" fillId="5" borderId="2" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 12: Totals Row Debit -->
    <xf numFmtId="164" fontId="3" fillId="4" borderId="2" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 13: Totals Row Credit -->
    <xf numFmtId="164" fontId="4" fillId="3" borderId="2" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 14: Totals Row Balance/Currency -->
    <xf numFmtId="0" fontId="2" fillId="5" borderId="2" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 15: Main Big Header Title -->
    <xf numFmtId="0" fontId="5" fillId="0" borderId="0" applyFont="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 16: Metadata Sub-Header Right -->
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" applyFont="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 17: Metadata Sub-Header Left -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" applyFont="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <!-- 18: Exchange Rate Badge Cell -->
    <xf numFmtId="0" fontId="0" fillId="7" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 19: Foreign Cash Badge Cell -->
    <xf numFmtId="0" fontId="0" fillId="8" borderId="1" applyFont="1" applyFill="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
  </cellXfs>
</styleSheet>"""
    }

    fun buildXlsxFile(
        sheetName: String,
        columns: List<SheetColumn>,
        rows: List<Row>,
        merges: List<MergeRange>,
        file: File
    ) {
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        val globalRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        val escapedSheetName = sheetName.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="$escapedSheetName" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val sbSheet = StringBuilder()
        sbSheet.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView rightToLeft="1" tabSelected="1" workbookViewId="0"/>
  </sheetViews>""")

        if (columns.isNotEmpty()) {
            sbSheet.append("\n  <cols>")
            for (col in columns) {
                sbSheet.append("\n    <col min=\"${col.min}\" max=\"${col.max}\" width=\"${col.width}\" customWidth=\"1\"/>")
            }
            sbSheet.append("\n  </cols>")
        }

        sbSheet.append("\n  <sheetData>")
        for (row in rows) {
            sbSheet.append("\n    ").append(row.toXml())
        }
        sbSheet.append("\n  </sheetData>")

        if (merges.isNotEmpty()) {
            sbSheet.append("\n  <mergeCells count=\"${merges.size}\">")
            for (m in merges) {
                sbSheet.append("\n    <mergeCell ref=\"${m.ref}\"/>")
            }
            sbSheet.append("\n  </mergeCells>")
        }

        sbSheet.append("\n</worksheet>")

        FileOutputStream(file).use { fos ->
            val zos = ZipOutputStream(fos)

            fun addZipEntry(path: String, content: String) {
                val entry = ZipEntry(path)
                zos.putNextEntry(entry)
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addZipEntry("[Content_Types].xml", contentTypes)
            addZipEntry("_rels/.rels", globalRels)
            addZipEntry("xl/workbook.xml", workbookXml)
            addZipEntry("xl/_rels/workbook.xml.rels", workbookRels)
            addZipEntry("xl/styles.xml", getStylesXml())
            addZipEntry("xl/worksheets/sheet1.xml", sbSheet.toString())

            zos.finish()
        }
    }
}
