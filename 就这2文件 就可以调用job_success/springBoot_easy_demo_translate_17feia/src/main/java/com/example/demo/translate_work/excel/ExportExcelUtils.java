package com.example.demo.translate_work.excel;

import com.example.demo.translate_work.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class ExportExcelUtils {

    /**
     * 导出单页sheet
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/18 15:21
     */
    public static void exportExcel(HttpServletResponse response, String fileName, ExcelData data) throws Exception {

        //设置请求头
        if (response != null) {
            setHeaders(response, fileName);
        }
        //导出excel
        if (response != null) {
            exportExcel(data, response.getOutputStream());
        } else {
            //文件夹
            String fileDirName = fileName.substring(0, fileName.lastIndexOf("/"));
            File fileDir = new File(fileDirName);
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }

            exportExcel(data, new FileOutputStream(new File(fileName)));
        }
    }

    /**
     * 设置请求头
     *
     * @param response
     * @param fileName
     * @throws UnsupportedEncodingException
     */
    private static void setHeaders(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        response.setHeader("Content-Disposition",
                "attachment;filename=" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));// 指定下载的文件名
        response.setContentType("application/octet-stream;charset=utf-8");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setCharacterEncoding("utf-8");
    }

    /**
     * 导出单页sheet
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/18 15:21
     */
    private static void exportExcel(ExcelData data, OutputStream out) throws Exception {

        HSSFWorkbook wb = new HSSFWorkbook();
        String sheetName = data.getName();
        if (null == sheetName) {
            sheetName = "Sheet1";
        }
        HSSFSheet sheet = wb.createSheet(sheetName);

        //获取所有sheet页个数
        int sheetTotal = wb.getNumberOfSheets();

        //@Author: Kevin 2018-11-19 15:20
        //@Descreption: 处理下拉数据 => 将数据放到隐藏sheet页中, 下拉框的数据从sheet页中取, 防止内容过长导致导出后下拉数据无法显示
        if (data.getCellRangeMap() != null) {
            Set<Map.Entry<Integer, String[]>> selectSet = data.getCellRangeMap().entrySet();
            Iterator iterator = ((Set) selectSet).iterator();

            while (iterator.hasNext()) {
                Map.Entry<Integer, String[]> entryMap = (Map.Entry<Integer, String[]>) iterator.next();
                Integer columnIndex = entryMap.getKey();
                String[] selectList = entryMap.getValue();

                //新建一个sheet页
                String hiddenSheetName = "hiddenSheet" + sheetTotal;
                HSSFSheet hiddenSheet = wb.createSheet(hiddenSheetName);
                Row row;

                //写入下拉数据到新的sheet页中
                for (int i = 0; i < selectList.length; i++) {
                    row = hiddenSheet.createRow(i);
                    Cell cell = row.createCell(0);
                    cell.setCellValue(new HSSFRichTextString(selectList[i]));
//                    cell.setCellValue(selectList[i]);
                }

                String strFormula = hiddenSheetName + "!$A$1:$A$65535";
                XSSFDataValidationConstraint constraint = new XSSFDataValidationConstraint(DataValidationConstraint.ValidationType.LIST, strFormula);
                // 设置数据有效性加载在哪个单元格上,四个参数分别是：起始行、终止行、起始列、终止列
                CellRangeAddressList regions = new CellRangeAddressList(0, 65535, columnIndex, columnIndex);
                // 数据有效性对象
                DataValidationHelper help = new HSSFDataValidationHelper((HSSFSheet) sheet);
                DataValidation validation = help.createValidation(constraint, regions);
                sheet.addValidationData(validation);

                //将新建的sheet页隐藏掉
                wb.setSheetHidden(sheetTotal, true);

                sheetTotal++;
            }

        }

        writeExcel(wb, sheet, data);

        wb.write(out);
        out.close();
    }

    /**
     * 导出多页sheet
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/18 15:25
     */
    public static void exportExcel(HttpServletResponse response, String fileName, List<ExcelData> excelDataList) throws Exception {

        //设置请求头
        setHeaders(response, fileName);

        OutputStream out = response.getOutputStream();
        HSSFWorkbook wb = new HSSFWorkbook();

        for (int i = 0; i < excelDataList.size(); i++) {
            ExcelData data = excelDataList.get(i);
            String sheetName = data.getName();
            if (StringUtils.isBlank(sheetName)) {
                sheetName = "Sheet" + (i + 1);
            }
            HSSFSheet sheet = wb.createSheet(sheetName);
            writeExcel(wb, sheet, data);
        }
        wb.write(out);
        out.close();
    }

    private static void writeExcel(HSSFWorkbook wb, Sheet sheet, ExcelData data) {
        int rowIndex = 0;
        int cellLeng = 0;
        if (data.getTitleList() != null && data.getTitleList().size() > 0) {
            rowIndex = writeTitlesToExcels(wb, sheet, data.getTitleList(), rowIndex);
            cellLeng = data.getTitleList().get(0).size() + 1;
        } else {
            rowIndex = writeTitlesToExcel(wb, sheet, data.getTitles(), rowIndex);
            cellLeng = data.getTitles().size() + 1;
        }
        setCellRange(sheet, data.getCellRangeMap(), rowIndex);
        rowIndex = writeRowsToExcel(wb, sheet, data.getRows(), rowIndex);
        if (data.getCellRangeArray() != null && data.getCellRangeArray().size() > 0) {
            setCelRanges(sheet, data.getCellRangeArray());
        }
        autoSizeColumns(sheet, cellLeng);
        if (data.getEndList() != null && data.getEndList().size() > 0) {
            writeEndsToExcels(wb, sheet, data.getEndList(), rowIndex);
        }
    }

    /**
     * tsd 2019-06-14
     * 设置数据合并
     *
     * @param sheet
     * @param ranges
     */
    private static void setCelRanges(Sheet sheet, List<Integer[]> ranges) {
        for (Integer[] rangeValue : ranges) {
            CellRangeAddress callRangeAddress = new CellRangeAddress(rangeValue[0], rangeValue[1], rangeValue[2], rangeValue[3]);
            sheet.addMergedRegion(callRangeAddress);
        }
    }

    /**
     * @param sheet
     * @param cellRangeMap 下拉数据
     * @Author: TuShiDing
     * 设置下拉框
     */
    private static void setCellRange(Sheet sheet, Map<Integer, String[]> cellRangeMap, int rowIndex) {
        if (cellRangeMap != null) {
            for (Map.Entry<Integer, String[]> entry : cellRangeMap.entrySet()) {
                int key = entry.getKey();
                String[] ranges = entry.getValue();
                cellList(sheet, ranges, rowIndex, 65535, key, key);
            }
        }
    }

    /**
     * @Author: TuShiDing
     * @Description: excel 下拉框数据校验
     * @Param:
     * @Date: 11:33 2018/10/24
     */
    public static void cellList(Sheet sheet, String[] strings, int firstRow, int endRow, int firstColumn, int endColumn) {
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstColumn, endColumn); //excel下拉菜单作用域
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(strings);
        DataValidation dataValidation = helper.createValidation(constraint, regions);

        //处理Excel兼容性问题
        if (dataValidation instanceof HSSFDataValidation) {
            dataValidation.setSuppressDropDownArrow(true);
            dataValidation.setShowErrorBox(true);
        } else {
            dataValidation.setSuppressDropDownArrow(false);
        }

        sheet.addValidationData(dataValidation);
    }

    private static int writeTitlesToExcels(HSSFWorkbook wb, Sheet sheet, List<List<String>> titleList, int rowIndex) {

        Font titleFont = wb.createFont();
        titleFont.setFontName("simsun");
//        titleFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        titleFont.setBold(true);
        // titleFont.setFontHeightInPoints((short) 14);
        titleFont.setColor(IndexedColors.BLACK.index);

        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setWrapText(true);
//        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setFillForegroundColor(HSSFColor.SKY_BLUE.index);
//        titleStyle.setFillForegroundColor(new HSSFColor(new CTColor(182, 184, 192)));
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setFont(titleFont);
        setBorder(titleStyle, BorderStyle.THIN, null);
        List<Integer[]> ranges = new ArrayList<>();
        List<Integer[]> rangesRow = new ArrayList<>();
        Map<String, Integer[]> setMapRow = new HashMap<>();
        for (List<String> titles : titleList) {
            Map<String, Integer[]> setMap = new HashMap<>();
            Row titleRow = sheet.createRow(rowIndex);
            int colIndex = 0;
            for (String field : titles) {
                Cell cell = titleRow.createCell(colIndex);
//                cell.setCellValue(field);
                cell.setCellValue(new HSSFRichTextString(field));
                cell.setCellStyle(titleStyle);
                if (field != null && !"".equals(field)) {

                    //列合并算法
                    if (setMap.get("L" + field) == null) {
                        Integer[] range = new Integer[4];
                        range[0] = rowIndex;
                        range[1] = rowIndex;
                        range[2] = colIndex;
                        range[3] = colIndex;
                        setMap.put("L" + field, range);
                    } else {
                        Integer[] range = setMap.get("L" + field);
                        if (colIndex - range[3] == 1) {
                            range[3] = colIndex;
                            setMap.put("L" + field, range);
                        }
                    }
                    //行合并算法
                    if (setMapRow.get("R" + field + colIndex) == null) {
                        if (rowIndex == 0) {
                            Integer[] range = new Integer[4];
                            range[0] = rowIndex;
                            range[1] = rowIndex;
                            range[2] = colIndex;
                            range[3] = colIndex;
                            setMapRow.put("R" + field + colIndex, range);
                        }
                    } else {
                        Integer[] range = setMapRow.get("R" + field + colIndex);
                        range[1] = rowIndex;
                        setMapRow.put("R" + field + colIndex, range);
                    }
                }
                colIndex++;
            }
            for (Integer[] rangeValue : setMap.values()) {
                if (rangeValue[2] != rangeValue[3]) {
                    ranges.add(rangeValue);
                }
            }
            for (Integer[] rangeValue : setMapRow.values()) {
                if (rangeValue[0] != rangeValue[1]) {
                    rangesRow.add(rangeValue);
                }
            }
            rowIndex++;
        }
        for (Integer[] rangeValue : ranges) {
            CellRangeAddress callRangeAddress = new CellRangeAddress(rangeValue[0], rangeValue[1], rangeValue[2], rangeValue[3]);
            sheet.addMergedRegion(callRangeAddress);
        }
        for (Integer[] rangeValue : rangesRow) {
            CellRangeAddress callRangeAddress = new CellRangeAddress(rangeValue[0], rangeValue[1], rangeValue[2], rangeValue[3]);
            sheet.addMergedRegion(callRangeAddress);
        }
        return rowIndex;
    }

    private static int writeEndsToExcels(HSSFWorkbook wb, Sheet sheet, List<List<Object>> endList, int rowIndex) {


        Font dataFont = wb.createFont();
        dataFont.setFontName("simsun");
        // dataFont.setFontHeightInPoints((short) 14);
        dataFont.setColor(IndexedColors.BLACK.index);

        HSSFCellStyle dataStyle = wb.createCellStyle();
        dataStyle.setWrapText(true);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setFont(dataFont);
        setBorder(dataStyle, BorderStyle.THIN, null);

        List<Integer[]> ranges = new ArrayList<>();
        List<Integer[]> rangesRow = new ArrayList<>();
        Map<String, Integer[]> setMapRow = new HashMap<>();
        for (List<Object> titles : endList) {
            Map<String, Integer[]> setMap = new HashMap<>();
            Row titleRow = sheet.createRow(rowIndex);
            int colIndex = 0;
            for (Object field : titles) {
                Cell cell = titleRow.createCell(colIndex);
//                cell.setCellValue(field + "");
                cell.setCellValue(new HSSFRichTextString(field + ""));
                if (field != null && !"".equals(field)) {

                    //列合并算法
                    if (setMap.get("L" + field) == null) {
                        Integer[] range = new Integer[4];
                        range[0] = rowIndex;
                        range[1] = rowIndex;
                        range[2] = colIndex;
                        range[3] = colIndex;
                        setMap.put("L" + field, range);
                    } else {
                        Integer[] range = setMap.get("L" + field);
                        if (colIndex - range[3] == 1) {
                            range[3] = colIndex;
                            setMap.put("L" + field, range);
                        }
                    }
                    //行合并算法
                    if (setMapRow.get("R" + field + colIndex) == null) {
                        if (rowIndex == 0) {
                            Integer[] range = new Integer[4];
                            range[0] = rowIndex;
                            range[1] = rowIndex;
                            range[2] = colIndex;
                            range[3] = colIndex;
                            setMapRow.put("R" + field + colIndex, range);
                        }
                    } else {
                        Integer[] range = setMapRow.get("R" + field + colIndex);
                        range[1] = rowIndex;
                        setMapRow.put("R" + field + colIndex, range);
                    }
                }
                colIndex++;
            }
            for (Integer[] rangeValue : setMap.values()) {
                if (rangeValue[2] != rangeValue[3]) {
                    ranges.add(rangeValue);
                }
            }
            for (Integer[] rangeValue : setMapRow.values()) {
                if (rangeValue[0] != rangeValue[1]) {
                    rangesRow.add(rangeValue);
                }
            }
            rowIndex++;
        }
        for (Integer[] rangeValue : ranges) {
            CellRangeAddress callRangeAddress = new CellRangeAddress(rangeValue[0], rangeValue[1], rangeValue[2], rangeValue[3]);
            sheet.addMergedRegion(callRangeAddress);
        }
        for (Integer[] rangeValue : rangesRow) {
            CellRangeAddress callRangeAddress = new CellRangeAddress(rangeValue[0], rangeValue[1], rangeValue[2], rangeValue[3]);
            sheet.addMergedRegion(callRangeAddress);
        }
        return rowIndex;
    }

    private static int writeTitlesToExcel(HSSFWorkbook wb, Sheet sheet, List<String> titles, int rowIndex) {

        Font titleFont = wb.createFont();
        titleFont.setFontName("simsun");
        titleFont.setBold(true);
//        titleFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        // titleFont.setFontHeightInPoints((short) 14);
        titleFont.setColor(IndexedColors.BLACK.index);

        HSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setWrapText(true);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setFillForegroundColor(HSSFColor.SKY_BLUE.index);
//        titleStyle.setFillForegroundColor(new HSSFColor(new CTColor(182, 184, 192)));
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setFont(titleFont);
        setBorder(titleStyle, BorderStyle.THIN, null);

        Row titleRow = sheet.createRow(rowIndex);
        // titleRow.setHeightInPoints(25);

        int colIndex = 0;
        for (String field : titles) {
            Cell cell = titleRow.createCell(colIndex);
            cell.setCellValue(new HSSFRichTextString(field));
//            cell.setCellValue(field);
            cell.setCellStyle(titleStyle);
            colIndex++;
        }

        rowIndex++;
        return rowIndex;
    }

    private static int writeRowsToExcel(HSSFWorkbook wb, Sheet sheet, List<List<Object>> rows, int rowIndex) {
        int colIndex = 0;

        Font dataFont = wb.createFont();
        dataFont.setFontName("simsun");
        // dataFont.setFontHeightInPoints((short) 14);
        dataFont.setColor(IndexedColors.BLACK.index);

        HSSFCellStyle dataStyle = wb.createCellStyle();
        dataStyle.setWrapText(true);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setFont(dataFont);
        setBorder(dataStyle, BorderStyle.THIN, null);

        for (List<Object> rowData : rows) {
            Row dataRow = sheet.createRow(rowIndex);
            // dataRow.setHeightInPoints(25);
            colIndex = 0;

            for (Object cellData : rowData) {
                try {
                    Cell cell = dataRow.createCell(colIndex);
                    if (cellData != null) {
                        cell.setCellValue(new HSSFRichTextString(cellData.toString()));
//                        cell.setCellValue(cellData.toString());
                    } else {
                        cell.setCellValue("");
                    }

                    cell.setCellStyle(dataStyle);
                    colIndex++;
                } catch (Exception e) {
                    System.out.print(colIndex);
                }
            }
            rowIndex++;
        }
        return rowIndex;
    }

    private static void autoSizeColumns(Sheet sheet, int columnNumber) {

        for (int i = 0; i < columnNumber; i++) {
            int colWidth = sheet.getColumnWidth(i) * 2;
            sheet.autoSizeColumn(i, true);
            if (colWidth < 255 * 256) {
                sheet.setColumnWidth(i, colWidth < 3000 ? 3000 : colWidth);
            } else {
                sheet.setColumnWidth(i, 6000);
            }
        }
    }

    private static void setBorder(CellStyle style, BorderStyle border, HSSFColor color) {
        style.setBorderTop(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
        style.setBorderBottom(border);
//        style.setBorderColor(HSSFCellBorder.BorderSide.TOP, color);
//        style.setBorderColor(HSSFCellBorder.BorderSide.LEFT, color);
//        style.setBorderColor(HSSFCellBorder.BorderSide.RIGHT, color);
//        style.setBorderColor(HSSFCellBorder.BorderSide.BOTTOM, color);
    }

    public static String transferCellType(Cell cell) {
        if (cell == null) {
            return "";
        }
        cell.setCellType(Cell.CELL_TYPE_STRING);
        return StringUtils.isNotBlank(cell.getStringCellValue()) ? cell.getStringCellValue().trim() : "";
    }

}
