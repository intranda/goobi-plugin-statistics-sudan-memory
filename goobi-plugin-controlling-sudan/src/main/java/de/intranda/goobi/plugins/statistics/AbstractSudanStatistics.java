package de.intranda.goobi.plugins.statistics;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IStatisticPlugin;

import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ProjectManager;
import de.sub.goobi.persistence.managers.StepManager;
import de.sub.goobi.persistence.managers.UsergroupManager;
import lombok.Data;
import lombok.extern.log4j.Log4j;

@Data
@Log4j
public abstract class AbstractSudanStatistics implements IStatisticPlugin {


    private PluginType type = PluginType.Statistics;

    private int projectId;
    private String filter;

    private Date startDate;
    private Date endDate;

    protected String startDateText;
    protected String endDateText;

    protected static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    protected List<Map<String, String>> resultList;

    private List<String> projectNames;
    protected String project;


    private List<String> stepNames;
    protected String step;


    private List<String> usergroupNames;
    protected String usergroup;

    protected String timeRange = "%Y-%m";


    @Override
    public String getData() {
        return null;
    }


    @Override
    public boolean getPermissions() {
        return true;
    }


    public void setStartDateText(String value) {
        startDateText = value;
    }

    public void setEndDateText(String value) {
        endDateText = value;
    }

    public String getStartDateAsString() {
        if (startDate != null) {
            return dateFormat.format(startDate);
        }
        return null;
    }

    public String getEndDateAsString() {
        if (endDate != null) {
            return dateFormat.format(endDate);
        }
        return null;
    }

    public List<String> getProjectNames() {
        if (projectNames == null || projectNames.isEmpty()) {
            projectNames = new ArrayList<>();
            projectNames.add("");
            projectNames.addAll(ProjectManager.getAllProjectTitles(false));
        }

        return projectNames;
    }

    public List<String> getStepNames() {
        if (stepNames == null || stepNames.isEmpty()) {
            stepNames = new ArrayList<>();
            stepNames.add("");
            stepNames.addAll(StepManager.getDistinctStepTitles());
        }
        return stepNames;
    }

    public List<String> getUsergroupNames() {
        if (usergroupNames == null || usergroupNames.isEmpty()) {
            usergroupNames = new ArrayList<>();
            usergroupNames.add("");
            usergroupNames.addAll(UsergroupManager.getAllUsergroupNames());
        }
        return usergroupNames;
    }

    public void resetStatistics() {
        resultList = null;
    }


    @Override
    public void calculate() {
        calculateData();
    }


    protected abstract void calculateData();

    public void generateExcelDownload() {
        if (resultList.isEmpty()) {
            Helper.setMeldung("No results to export.");
            return;
        }
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = wb.createSheet("results");

        // create header
        Row headerRow = sheet.createRow(0);
        Set<String> columnHeader = resultList.get(0).keySet();
        int columnCounter = 0;
        for (String headerName : columnHeader) {
            headerRow.createCell(columnCounter).setCellValue(Helper.getTranslation(headerName));
            columnCounter = columnCounter +1;
        }

        int rowCounter = 1;
        // add results
        for (Map<String, String> result : resultList) {
            Row resultRow = sheet.createRow(rowCounter);
            columnCounter = 0;
            for (String headerName : columnHeader) {
                resultRow .createCell(columnCounter).setCellValue(result.get(headerName));
                columnCounter = columnCounter +1;
            }
            rowCounter = rowCounter +1;
        }

        // write result into output stream
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();

        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        OutputStream out;
        try {
            out = response.getOutputStream();
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment;filename=\"report.xlsx\"");
            wb.write(out);
            out.flush();

            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        }
        try {
            wb.close();
        } catch (IOException e) {
            log.error(e);
        }
    }
}
