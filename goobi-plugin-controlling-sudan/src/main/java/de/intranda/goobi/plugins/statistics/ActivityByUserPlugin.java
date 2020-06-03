package de.intranda.goobi.plugins.statistics;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IStatisticPlugin;

import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ControllingManager;
import de.sub.goobi.persistence.managers.ProjectManager;
import de.sub.goobi.persistence.managers.StepManager;
import de.sub.goobi.persistence.managers.UsergroupManager;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@Log4j2
@Data
@PluginImplementation
public class ActivityByUserPlugin implements IStatisticPlugin {

    private String title = "plugin_statistics_sudan_activity_by_user";

    private PluginType type = PluginType.Statistics;

    private int projectId;
    private String filter;

    private Date startDate;
    private Date endDate;

    private String startDateText;
    private String endDateText;

    private Date startDateDate;
    private Date endDateDate;


    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private List<Map<String, String>> resultListDetails;
    private List<Map<String, String>> resultListOverview;

    private List<String> projectNames;
    private String project;

    private List<String> stepNames;
    private String step;

    private List<String> usergroupNames;
    private String usergroup;

    private String timeRange = "%Y-%m";
    private List<String> headerOrderOverview = new ArrayList<>();
    private List<String> headerOrderDetails = new ArrayList<>();


    @Override
    public String getGui() {
        return "/uii/plugin_statistics_sudan_activity_by_user.xhtml";
    }

    public ActivityByUserPlugin() {
        headerOrderOverview.add("plugin_statistics_sudan_timeRange");
        headerOrderOverview.add("plugin_statistics_sudan_titleCount");
        headerOrderOverview.add("plugin_statistics_sudan_titlearabicCount");
        headerOrderOverview.add("plugin_statistics_sudan_descriptionCount");
        headerOrderOverview.add("plugin_statistics_sudan_descriptionarabicCount");
        headerOrderOverview.add("plugin_statistics_sudan_workflowTitleCount");
        headerOrderOverview.add("plugin_statistics_sudan_workflowTitle");
        headerOrderOverview.add("plugin_statistics_sudan_userName");

        headerOrderDetails.add("plugin_statistics_sudan_title");
        headerOrderDetails.add("plugin_statistics_sudan_titleCount");
        headerOrderDetails.add("plugin_statistics_sudan_titlearabic");
        headerOrderDetails.add("plugin_statistics_sudan_titlearabicCount");
        headerOrderDetails.add("plugin_statistics_sudan_description");
        headerOrderDetails.add("plugin_statistics_sudan_descriptionCount");
        headerOrderDetails.add("plugin_statistics_sudan_descriptionarabic");
        headerOrderDetails.add("plugin_statistics_sudan_descriptionarabicCount");
        headerOrderDetails.add("plugin_statistics_sudan_workflowTitle");
        headerOrderDetails.add("plugin_statistics_sudan_processTitle");
        headerOrderDetails.add("plugin_statistics_sudan_userName");

    }

    @Override
    public void calculate() {
        calculateData();
    }

    /*

    SELECT
    m1.processid,
    m1.value AS plugin_statistics_sudan_title,
    WORDCOUNT(m1.value) AS plugin_statistics_sudan_titleCount,
    m2.value AS plugin_statistics_sudan_titlearabic,
    WORDCOUNT(m2.value) AS plugin_statistics_sudan_titlearabicCount,
    m3.value AS plugin_statistics_sudan_description,
    WORDCOUNT(m3.value) AS plugin_statistics_sudan_descriptionCount,
    m4.value AS plugin_statistics_sudan_descriptionarabic,
    WORDCOUNT(m4.value) AS plugin_statistics_sudan_descriptionarabicCount,
    s.Titel AS plugin_statistics_sudan_workflowTitle,
    p.Titel AS plugin_statistics_sudan_processTitle,
    CONCAT(u.Nachname, ', ', u.Vorname) AS plugin_statistics_sudan_userName
    FROM
    metadata m1
        JOIN
    metadata m2 ON m1.processid = m2.processid
        JOIN
    metadata m3 ON m1.processid = m3.processid
        JOIN
    metadata m4 ON m1.processid = m4.processid
        JOIN
    schritte s ON m1.processid = s.ProzesseID
        LEFT JOIN
    prozesse p ON s.ProzesseID = p.ProzesseID
        LEFT JOIN
    benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID
    WHERE
    m1.name = 'TitleDocMain'
        AND m2.name = 'TitleDocMainArabic'
        AND m3.name = 'ContentDescription'
        AND m4.name = 'ContentDescriptionArabic'
        AND s.typMetadaten = TRUE
        AND s.titel like '%ranslat%'
        AND s.Bearbeitungsstatus = 3
        AND s.BearbeitungsEnde BETWEEN '2019-01-01' AND '2020-12-31';


    SELECT
    DATE_FORMAT(s.BearbeitungsEnde, '%Y-%m') AS plugin_statistics_sudan_timeRange,
    WORDCOUNT(GROUP_CONCAT(m1.value SEPARATOR ' ')) AS plugin_statistics_sudan_titleCount,
    WORDCOUNT(GROUP_CONCAT(m2.value SEPARATOR ' ')) AS plugin_statistics_sudan_titlearabicCount,
    WORDCOUNT(GROUP_CONCAT(m3.value SEPARATOR ' ')) AS plugin_statistics_sudan_descriptionCount,
    WORDCOUNT(GROUP_CONCAT(m4.value SEPARATOR ' ')) AS plugin_statistics_sudan_descriptionarabicCount,
    COUNT(s.Titel) AS plugin_statistics_sudan_workflowTitleCount,
    CONCAT(u.Nachname, ', ', u.Vorname) AS plugin_statistics_sudan_userName
    FROM
    metadata m1
        JOIN
    metadata m2 ON m1.processid = m2.processid
        JOIN
    metadata m3 ON m1.processid = m3.processid
        JOIN
    metadata m4 ON m1.processid = m4.processid
        JOIN
    schritte s ON m1.processid = s.ProzesseID
        LEFT JOIN
    benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID
    WHERE
    m1.name = 'TitleDocMain'
        AND m2.name = 'TitleDocMainArabic'
        AND m3.name = 'ContentDescription'
        AND m4.name = 'ContentDescriptionArabic'
        AND s.typMetadaten = TRUE
        AND s.Bearbeitungsstatus = 3
        AND s.titel like '%ranslat%'
        AND s.BearbeitungsEnde BETWEEN '2019-01-01' AND '2020-12-31'
    GROUP BY plugin_statistics_sudan_timeRange , plugin_statistics_sudan_userName;


    drop function wordcount;
    DELIMITER $$
    CREATE FUNCTION wordcount(str TEXT CHARSET utf8mb4)
            RETURNS INT
            DETERMINISTIC
            SQL SECURITY INVOKER
            NO SQL
       BEGIN
         DECLARE wordCnt, idx, maxIdx INT DEFAULT 0;
         DECLARE currChar, prevChar BOOL DEFAULT 0;
         SET maxIdx=char_length(str);
         WHILE idx < maxIdx DO
             SET currChar=SUBSTRING(str, idx, 1) RLIKE '[[:alnum:]]';
             IF NOT prevChar AND currChar THEN
                 SET wordCnt=wordCnt+1;
             END IF;
             SET prevChar=currChar;
             SET idx=idx+1;
         END WHILE;
         RETURN wordCnt;
       END
     $$
     DELIMITER ;

     */

    /**
     * calculate data from database
     */

    private void calculateData() {

        if (startDateDate != null) {
            startDateText = dateFormat.format(endDateDate);
        }
        if (startDateDate != null) {
            endDateText = dateFormat.format(endDateDate);
        }
        System.out.println(startDateText);
        System.out.println(endDateText);
        resultListDetails = null;
        StringBuilder overview = new StringBuilder();

        overview.append("SELECT ");
        overview.append("DATE_FORMAT(s.BearbeitungsEnde, ");
        overview.append(timeRange);
        overview.append(") AS plugin_statistics_sudan_timeRange, ");
        overview.append("WORDCOUNT(GROUP_CONCAT(m1.value SEPARATOR ' ')) AS plugin_statistics_sudan_titleCount, ");
        overview.append("WORDCOUNT(GROUP_CONCAT(m2.value SEPARATOR ' ')) AS plugin_statistics_sudan_titlearabicCount, ");
        overview.append("WORDCOUNT(GROUP_CONCAT(m3.value SEPARATOR ' ')) AS plugin_statistics_sudan_descriptionCount, ");
        overview.append("WORDCOUNT(GROUP_CONCAT(m4.value SEPARATOR ' ')) AS plugin_statistics_sudan_descriptionarabicCount, ");
        overview.append("COUNT(s.Titel) AS plugin_statistics_sudan_workflowTitleCount, ");
        overview.append("s.Titel AS plugin_statistics_sudan_workflowTitle, ");
        overview.append("CONCAT(u.Nachname, ', ', u.Vorname) AS plugin_statistics_sudan_userName ");
        overview.append("FROM ");
        overview.append("metadata m1 ");
        overview.append("    JOIN ");
        overview.append(" metadata m2 ON m1.processid = m2.processid ");
        overview.append("    JOIN ");
        overview.append("metadata m3 ON m1.processid = m3.processid ");
        overview.append("    JOIN ");
        overview.append("metadata m4 ON m1.processid = m4.processid ");
        overview.append("    JOIN ");
        overview.append("schritte s ON m1.processid = s.ProzesseID ");
        overview.append("    LEFT JOIN ");
        overview.append("benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID ");
        overview.append("WHERE ");
        overview.append("m1.name = 'TitleDocMain' ");
        overview.append("    AND m2.name = 'TitleDocMainArabic' ");
        overview.append("    AND m3.name = 'ContentDescription' ");
        overview.append("    AND m4.name = 'ContentDescriptionArabic' ");
        overview.append("    AND s.typMetadaten = TRUE ");
        overview.append("    AND s.Bearbeitungsstatus = 3 ");
        overview.append("    AND s.titel like '%ranslat%' ");

        if (StringUtils.isNotBlank(startDateText) && StringUtils.isNotBlank(endDateText)) {
            overview.append("AND s.BearbeitungsEnde BETWEEN '");
            overview.append(startDateText);
            overview.append("' and '");
            overview.append(endDateText);
            overview.append("' ");
        } else if (StringUtils.isNotBlank(startDateText)) {
            overview.append("AND s.BearbeitungsEnde >= '");
            overview.append(startDateText);
            overview.append("' ");
        } else if (StringUtils.isNotBlank(endDateText)) {
            overview.append("AND s.BearbeitungsEnde <= '");
            overview.append(endDateText);
            overview.append("' ");
        }

        overview.append("GROUP BY plugin_statistics_sudan_timeRange, plugin_statistics_sudan_userName, plugin_statistics_sudan_workflowTitle; ");

        resultListOverview = ControllingManager.getResultsAsMaps(overview.toString());

    }

    public void calculateDetailData() {
        if (startDateDate != null) {
            startDateText = dateFormat.format(endDateDate);
        }
        if (startDateDate != null) {
            endDateText = dateFormat.format(endDateDate);
        }


        resultListOverview = null;
        StringBuilder details = new StringBuilder();
        details.append("SELECT ");
        //        details.append("m1.processid, ");
        details.append("m1.value AS plugin_statistics_sudan_title, ");
        details.append("WORDCOUNT(m1.value) AS plugin_statistics_sudan_titleCount, ");
        details.append("m2.value AS plugin_statistics_sudan_titlearabic, ");
        details.append("WORDCOUNT(m2.value) AS plugin_statistics_sudan_titlearabicCount, ");
        details.append("m3.value AS plugin_statistics_sudan_description, ");
        details.append("WORDCOUNT(m3.value) AS plugin_statistics_sudan_descriptionCount, ");
        details.append("m4.value AS plugin_statistics_sudan_descriptionarabic, ");
        details.append("WORDCOUNT(m4.value) AS plugin_statistics_sudan_descriptionarabicCount, ");
        details.append("s.Titel AS plugin_statistics_sudan_workflowTitle, ");
        details.append("p.Titel AS plugin_statistics_sudan_processTitle, ");
        details.append("CONCAT(u.Nachname, ', ', u.Vorname) AS plugin_statistics_sudan_userName ");
        details.append("FROM ");
        details.append("metadata m1 ");
        details.append("    JOIN ");
        details.append("metadata m2 ON m1.processid = m2.processid ");
        details.append("    JOIN ");
        details.append("metadata m3 ON m1.processid = m3.processid ");
        details.append("    JOIN ");
        details.append("metadata m4 ON m1.processid = m4.processid ");
        details.append("    JOIN ");
        details.append("schritte s ON m1.processid = s.ProzesseID ");
        details.append("    LEFT JOIN ");
        details.append("prozesse p ON s.ProzesseID = p.ProzesseID ");
        details.append("    LEFT JOIN ");
        details.append("benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID ");
        details.append("WHERE ");
        details.append("m1.name = 'TitleDocMain' ");
        details.append("    AND m2.name = 'TitleDocMainArabic' ");
        details.append("    AND m3.name = 'ContentDescription' ");
        details.append("   AND m4.name = 'ContentDescriptionArabic' ");
        details.append("    AND s.typMetadaten = TRUE ");
        details.append("    AND s.titel like '%ranslat%' ");
        details.append("    AND s.Bearbeitungsstatus = 3 ");

        if (StringUtils.isNotBlank(startDateText) && StringUtils.isNotBlank(endDateText)) {
            details.append("AND s.BearbeitungsEnde BETWEEN '");
            details.append(startDateText);
            details.append("' and '");
            details.append(endDateText);
            details.append("' ");
        } else if (StringUtils.isNotBlank(startDateText)) {
            details.append("AND s.BearbeitungsEnde >= '");
            details.append(startDateText);
            details.append("' ");
        } else if (StringUtils.isNotBlank(endDateText)) {
            details.append("AND s.BearbeitungsEnde <= '");
            details.append(endDateText);
            details.append("' ");
        }

        details.append(" ORDER BY ");
        details.append("DATE_FORMAT(s.BearbeitungsEnde, ");
        details.append(timeRange);
        details.append("), plugin_statistics_sudan_userName ");
        resultListDetails = ControllingManager.getResultsAsMaps(details.toString());
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
        resultListOverview = null;
        resultListDetails = null;
        startDate=null;
        endDate=null;
        startDateText=null;
        endDateText=null;
    }

    public void generateExcelDownload() {
        List<Map<String, String>> resultList = null;
        List<String> headerOrder = null;
        if (resultListDetails != null) {
            resultList = resultListDetails;
            headerOrder = headerOrderDetails;
        } else if (resultListOverview != null) {
            resultList = resultListOverview;
            headerOrder = headerOrderOverview;
        }

        if (resultList == null || resultList.isEmpty()) {
            Helper.setMeldung("No results to export.");
            return;
        }
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = wb.createSheet("results");

        // create header
        Row headerRow = sheet.createRow(0);

        int columnCounter = 0;
        for (String headerName : headerOrder) {
            headerRow.createCell(columnCounter).setCellValue(Helper.getTranslation(headerName));
            columnCounter = columnCounter + 1;
        }

        int rowCounter = 1;
        // add results
        for (Map<String, String> result : resultList) {
            Row resultRow = sheet.createRow(rowCounter);
            columnCounter = 0;
            for (String headerName : headerOrder) {
                resultRow.createCell(columnCounter).setCellValue(result.get(headerName));
                columnCounter = columnCounter + 1;
            }
            rowCounter = rowCounter + 1;
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

    @Override
    public String getData() {
        return null;
    }
}
