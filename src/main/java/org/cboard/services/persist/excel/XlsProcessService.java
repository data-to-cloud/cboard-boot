package org.cboard.services.persist.excel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.cboard.dao.BoardDao;
import org.cboard.pojo.DashboardBoard;
import org.cboard.services.persist.PersistContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by yfyuan on 2017/2/15.
 */
@Service
public class XlsProcessService {

    @Autowired
    private BoardDao boardDao;

    private XlsProcesser jpgXlsProcesser = new JpgXlsProcesser();
    private XlsProcesser tableXlsProcesser = new TableXlsProcesser();

    public HSSFWorkbook dashboardToXls(List<PersistContext> contexts) {
        XlsProcesserContext context = null;
        for (PersistContext e : contexts) {
            context = dashboardToXls(e, context);
        }
        return (HSSFWorkbook) context.getWb();
    }



    public void tableToXlsx(JSONObject data){

    }

    public HSSFWorkbook tableToxls(JSONObject data) {
        XlsProcesserContext context = new XlsProcesserContext();
        HSSFWorkbook wb = new HSSFWorkbook();
        setColorIndex(wb);
        CellStyle titleStyle = createTitleStyle(wb);
        CellStyle thStyle = createThStyle(wb);
        CellStyle tStyle = createTStyle(wb);
        CellStyle percentStyle = wb.createCellStyle();
        percentStyle.cloneStyleFrom(tStyle);
        percentStyle.setDataFormat((short) 0xa);
        context.setWb(wb);
        context.setTableStyle(thStyle);
        context.setTitleStyle(titleStyle);
        context.settStyle(tStyle);
        context.setPercentStyle(percentStyle);
        //TODO data 超过6万条报错,创建新的sheet。
        Sheet sheet = context.getWb().createSheet();
        context.setBoardSheet(sheet);
        context.setC1(0);
        context.setC2(data.getJSONArray("data").getJSONArray(0).size() - 1);
        context.setR1(0);
        context.setR2(0);
        context.setData(data);
        new TableXlsProcesser().drawContent(context);
        setAutoWidth(sheet);
        return wb;
    }

    private XlsProcesserContext dashboardToXls(PersistContext persistContext, XlsProcesserContext context) {
        DashboardBoard board = boardDao.getBoard(persistContext.getDashboardId());
        JSONArray rows = JSONObject.parseObject(board.getLayout()).getJSONArray("rows");
        List<JSONArray> widgetRows = rows.stream().map(row -> (JSONObject) row)
                .filter(row -> row.getString("type") == null || "widget".equals(row.getString("type")))
                .map(row -> {
                    JSONArray widgets = row.getJSONArray("widgets");
                    widgets.forEach(a -> ((JSONObject) a).put("height", row.get("height")));
                    return widgets;
                })
                .collect(Collectors.toList());

        int widgets = 0;
        int tables = 0;
        for (JSONArray rw : widgetRows) {
            for (int i = 0; i < rw.size(); i++) {
                JSONObject widget = rw.getJSONObject(i);
                JSONObject v = persistContext.getData().getJSONObject(widget.getLong("widgetId").toString());
                if (v != null && "table".equals(v.getString("type"))) {
                    tables++;
                }
                widgets++;
            }
        }

        int columns = 170;
        int columnWidth = 1700 / columns;
        int column_width12 = 148;

        if (context == null) {
            context = new XlsProcesserContext();
            HSSFWorkbook wb = new HSSFWorkbook();
            setColorIndex(wb);
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle thStyle = createThStyle(wb);
            CellStyle tStyle = createTStyle(wb);
            CellStyle percentStyle = wb.createCellStyle();
            percentStyle.cloneStyleFrom(tStyle);
            percentStyle.setDataFormat((short) 0xa);
            context.setWb(wb);
            context.setTableStyle(thStyle);
            context.setTitleStyle(titleStyle);
            context.settStyle(tStyle);
            context.setPercentStyle(percentStyle);
        }
        int eachRow = -2;
        int dCol;
        int dRow;
        int widthInRow;

        if (tables != widgets) {
            Sheet sheet = context.getWb().createSheet(board.getName());
            sheet.setDisplayGridlines(false);
            IntStream.range(0, 180).forEach(i -> sheet.setColumnWidth(i, 365));
            context.setBoardSheet(sheet);
            for (JSONArray rw : widgetRows) {
                dCol = Math.round(30.0f / 1700 * columns);
                dRow = eachRow + 3;
                widthInRow = 0;
                for (int i = 0; i < rw.size(); i++) {

                    JSONObject widget = rw.getJSONObject(i);
                    JSONObject v = persistContext.getData().getJSONObject(widget.getLong("widgetId").toString());
                    if (v == null || v.keySet().size() == 0) {
                        continue;
                    }
                    int width = widget.getInteger("width").intValue();
                    int widget_cols = Math.round(1.0f * width / 12 * (148 - (rw.size() - 1) * 2));
                    widthInRow += width;
                    if (widthInRow > 12) {
                        dCol = Math.round(30.0f / 1700 * columns);
                        dRow = eachRow + 3;
                        widthInRow = width;
                    }
                    context.setC1(dCol + 2);
                    context.setC2(dCol + 2 + widget_cols);
                    context.setR1(dRow);
                    context.setR2(dRow);
                    context.setWidget(widget);
                    context.setData(v);
                    XlsProcesser processer = getProcesser(v.getString("type"));
                    ClientAnchor anchor = processer.draw(context);
                    if (anchor.getRow2() > eachRow) {
                        eachRow = anchor.getRow2();
                    }
                    dCol = context.getC2();
                }
            }
        }
        if (tables == 0) {
            return context;
        }
        dRow = 0;
        Sheet dataSheet = context.getWb().createSheet(board.getName() + "_table");
        context.setBoardSheet(dataSheet);
        for (JSONArray rw : widgetRows) {
            for (int i = 0; i < rw.size(); i++) {
                JSONObject widget = rw.getJSONObject(i);
                JSONObject v = persistContext.getData().getJSONObject(widget.getLong("widgetId").toString());
                if (v == null || !"table".equals(v.getString("type"))) {
                    continue;
                }
                context.setC1(0);
                int c2 = v.getJSONArray("data").getJSONArray(0).size() - 1;
                context.setC2(c2 == 0 ? 1 : c2);
                context.setR1(dRow);
                context.setR2(dRow);
                context.setWidget(widget);
                context.setData(v);
                XlsProcesser processer = getProcesser(v.getString("type"));
                ClientAnchor anchor = processer.draw(context);
                dRow = anchor.getRow2() + 2;
            }
        }
        setAutoWidth(dataSheet);

        return context;
    }

    private void setAutoWidth(Sheet dataSheet) {
        int max = 0;
        Iterator<Row> i = dataSheet.rowIterator();
        while (i.hasNext()) {
            Row r = i.next();
            if (r.getLastCellNum() > max) {
                max = r.getLastCellNum();
            }
        }
        for (int colNum = 0; colNum < max; colNum++) {
            dataSheet.autoSizeColumn(colNum, true);
        }
    }


    private XlsProcesser getProcesser(String type) {
        switch (type) {
            case "jpg":
                return jpgXlsProcesser;
            case "table":
                return tableXlsProcesser;
        }
        return null;
    }

    private CellStyle createTitleStyle(HSSFWorkbook wb) {
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontName("微软雅黑");
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setFont(font);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return titleStyle;
    }
//    private CellStyle createTitleStyleXlsx(BigExcelWriter writer) {
//
//        CellStyle titleStyle = writer.getCellStyle();
//        titleStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
//        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        titleStyle.setFont(font);
//        titleStyle.setAlignment(HorizontalAlignment.CENTER);
//        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//
//        return titleStyle;
//    }



    private CellStyle createThStyle(HSSFWorkbook wb) {
        CellStyle thStyle = wb.createCellStyle();
        thStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        thStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        thStyle.setBorderBottom(BorderStyle.THIN);
        thStyle.setBottomBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderLeft(BorderStyle.THIN);
        thStyle.setLeftBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderRight(BorderStyle.THIN);
        thStyle.setRightBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderTop(BorderStyle.THIN);
        thStyle.setTopBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setAlignment(HorizontalAlignment.CENTER);
        thStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        thStyle.setShrinkToFit(true);
        Font font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        thStyle.setFont(font);
        return thStyle;
    }
    private CellStyle createThStyleXlsx(BigExcelWriter writer) {
        CellStyle thStyle = writer.getCellStyle();
        thStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        thStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        thStyle.setBorderBottom(BorderStyle.THIN);
        thStyle.setBottomBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderLeft(BorderStyle.THIN);
        thStyle.setLeftBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderRight(BorderStyle.THIN);
        thStyle.setRightBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderTop(BorderStyle.THIN);
        thStyle.setTopBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setAlignment(HorizontalAlignment.CENTER);
        thStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        thStyle.setShrinkToFit(true);
        Font font = writer.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        thStyle.setFont(font);
        return thStyle;
    }



    private CellStyle createTStyle(HSSFWorkbook wb) {
        CellStyle tStyle = wb.createCellStyle();
        tStyle.setBorderBottom(BorderStyle.THIN);
        tStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderLeft(BorderStyle.THIN);
        tStyle.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderRight(BorderStyle.THIN);
        tStyle.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderTop(BorderStyle.THIN);
        tStyle.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setAlignment(HorizontalAlignment.CENTER);
        tStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        tStyle.setShrinkToFit(true);
        return tStyle;
    }
    private CellStyle createTStyleXlsx(ExcelWriter writer) {
        CellStyle tStyle = writer.getCellStyle();
        tStyle.setBorderBottom(BorderStyle.THIN);
        tStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderLeft(BorderStyle.THIN);
        tStyle.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderRight(BorderStyle.THIN);
        tStyle.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderTop(BorderStyle.THIN);
        tStyle.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setAlignment(HorizontalAlignment.CENTER);
        tStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        tStyle.setShrinkToFit(true);
        return tStyle;
    }

    private void setColorIndex(HSSFWorkbook wb) {
        HSSFPalette customPalette = wb.getCustomPalette();
        customPalette.setColorAtIndex(IndexedColors.BLUE.index, (byte) 26, (byte) 127, (byte) 205);
        customPalette.setColorAtIndex(IndexedColors.BLUE_GREY.index, (byte) 56, (byte) 119, (byte) 166);
        customPalette.setColorAtIndex(IndexedColors.GREY_25_PERCENT.index, (byte) 235, (byte) 235, (byte) 235);
    }

    public void data2list(ExcelWriter writer , JSONObject datas) {
//        BigExcelWriter writer = ExcelUtil.getBigWriter("./table.xlsx");
        List<Object> dataList = datas.getJSONArray("data");
        XlsxProcesserContext context = new XlsxProcesserContext();
        List<List<Object>> rows = new ArrayList<>();


        // 表头列表
        List<List<JSONObject>> titleList = new ArrayList();


        for(int i = 0; i < dataList.size(); i++) {
            List<Object> cell = new ArrayList<>();
            List<JSONObject> titleMap = new ArrayList<>();

            JSONArray arr = (JSONArray) dataList.get(i);
            Sheet sheet = writer.getSheet();
            for(int j = 0; j < arr.size(); j++) {
                JSONObject obj = arr.getJSONObject(j);
                String property = obj.getString("property");
                Object data = obj.get("data");


                sheet.setColumnWidth(j,(short)(18 *256));
                switch (property) {
                    case "header_key" :
                        titleMap.add(obj);
                        break;
                    case "header_empty" :
                        titleMap.add(obj);
                        break;
                    case "column_key" :
                    default:
                        cell.add(data);
                        break;
                }

            }
            if(!titleMap.isEmpty()) {
                writer.writeHeadRow(titleMap.stream().map(d-> ((JSONObject)d).getString("data")).collect(Collectors.toList()));
                titleList.add(titleMap);
            }
            if(!cell.isEmpty()){
                //总数据
                rows.add(cell);
            }
        }

        writer.write(rows);

        //处理合并
        for(int i = 0; i < titleList.size() ; i++) {
            List<JSONObject> titleMap = titleList.get(i);

            //存索引
            Map<String,Object> indexMap = new HashMap<>();


            //合并个数
            Map<String,Integer> result = new HashMap();

            int startIndex = titleMap.size(); //合并出使索引
            int endIndex = titleMap.size();   //合并结束索引
            indexMap.put("startIndex",startIndex);
            indexMap.put("endIndex",endIndex);

            for(int j = (titleMap.size()-1) ; j >= 0 ; j--) {
                JSONObject obj = titleMap.get(j);
                String data = obj.getString("data");

                if(result.containsKey(data)) {
                    startIndex = j;
                    result.put(data,result.get(data) + 1);

                }else {
                    result.put(data,1); //合并数量

                    if(startIndex != endIndex) {
                        //合并单元格
                        if(result.get(indexMap.get("flag")) == 1) {

                        } else {
                            endIndex = startIndex + result.get(indexMap.get("flag")) - 1;
                            writer.merge(i,i,startIndex,endIndex,indexMap.get("flag"), true);
                        }
                        //初始化index
                        startIndex = endIndex = j;

                    }else {

                    }
                    result.remove(indexMap.get("flag"));


                }
                indexMap.put("flag",data); //合并标志
            }
        }

        setCellStyle(writer);

        setHeadCellStyle(writer);


//
////        设置头样式
//        for(int i = 0; i < titleList.size() ; i++) {
//            CellStyle rowStyle = row.getRowStyle();
//            rowStyle.setFillBackgroundColor(IndexedColors.BLUE.getIndex());
//            Font font = writer.createFont();
//            font.setColor(IndexedColors.WHITE.getIndex());
//            rowStyle.setFont(font);
//            row.setRowStyle(rowStyle);
//
//
////            rowStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
////            rowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
////            rowStyle.setBorderBottom(BorderStyle.THIN);
////            rowStyle.setBottomBorderColor(IndexedColors.BLUE_GREY.getIndex());
////            rowStyle.setBorderLeft(BorderStyle.THIN);
////            rowStyle.setLeftBorderColor(IndexedColors.BLUE_GREY.getIndex());
////            rowStyle.setBorderRight(BorderStyle.THIN);
////            rowStyle.setRightBorderColor(IndexedColors.BLUE_GREY.getIndex());
////            rowStyle.setBorderTop(BorderStyle.THIN);
////            rowStyle.setTopBorderColor(IndexedColors.BLUE_GREY.getIndex());
////            rowStyle.setAlignment(HorizontalAlignment.CENTER);
////            rowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
////            rowStyle.setShrinkToFit(true);
////            Font font = writer.createFont();
////            font.setColor(IndexedColors.WHITE.getIndex());
////            rowStyle.setFont(font);
//        }


        System.out.println("成功");

    }

    private void setHeadCellStyle(ExcelWriter writer) {
        CellStyle thStyle = writer.getHeadCellStyle();
        thStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        thStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        thStyle.setBorderBottom(BorderStyle.THIN);
        thStyle.setBottomBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderLeft(BorderStyle.THIN);
        thStyle.setLeftBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderRight(BorderStyle.THIN);
        thStyle.setRightBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setBorderTop(BorderStyle.THIN);
        thStyle.setTopBorderColor(IndexedColors.BLUE_GREY.getIndex());
        thStyle.setAlignment(HorizontalAlignment.CENTER);
        thStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = writer.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontName("微软雅黑");
        thStyle.setFont(font);
    }

    private void setCellStyle(ExcelWriter writer) {
        CellStyle tStyle = writer.getCellStyle();
        tStyle.setBorderBottom(BorderStyle.THIN);
        tStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderLeft(BorderStyle.THIN);
        tStyle.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderRight(BorderStyle.THIN);
        tStyle.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setBorderTop(BorderStyle.THIN);
        tStyle.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tStyle.setAlignment(HorizontalAlignment.CENTER);
        tStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    }


    public static void main(String[] args) {
        BigExcelWriter writer = ExcelUtil.getBigWriter("./writeBeanTest.xlsx");
        //自定义标题别名
//        writer.addHeaderAlias("name", "姓名");
//        writer.addHeaderAlias("age", "年龄");
//        writer.addHeaderAlias("score", "分数");
//        writer.addHeaderAlias("isPass", "是否通过");
////        writer.merge(4, "一班成绩单");
//        Map map = new HashMap<>();
//        map.put("name", "姓名");
//        map.put("age", "年龄");
//        map.put("score", "分数");
//        map.put("isPass", "是否通过");
//
//
//        List<String> row1 = CollUtil.newArrayList("aa", "bb", "cc", "dd");
//        List<String> row2 = CollUtil.newArrayList("aa1", "bb1", "cc1", "dd1");
//        List<String> row3 = CollUtil.newArrayList("aa2", "bb2", "cc2", "dd2");
//        List<String> row4 = CollUtil.newArrayList("aa3", "bb3", "cc3", "dd3");
//        List<String> row5 = CollUtil.newArrayList("aa4", "bb4", "cc4", "dd4");
//
//
//        List<List<String>> rows = CollUtil.newArrayList(row1, row2, row3, row4, row5);
        // 一次性写出内容，使用默认样式
//        writer.setHeaderAlias(map);
//        writer.write(rows);
        // 关闭writer，释放内存
//        writer.close();



        Map<String, Object> row11 = new LinkedHashMap<>();
        row11.put("姓名", "张三");
        row11.put("年龄", 23);
        row11.put("成绩", 88.32);
        row11.put("是否合格", true);
        row11.put("考试日期", "321");

        Map<String, Object> row22 = new LinkedHashMap<>();
        row22.put("姓名", "李四");
        row22.put("年龄", 33);
        row22.put("成绩", 59.50);
        row22.put("是否合格", false);
        row22.put("考试日期", "123");

        ArrayList<Map<String, Object>> rows = CollUtil.newArrayList(row11, row22);

        writer.write(rows);
        writer.close();
    }
}
