package de.intranda.goobi.plugins.statistics;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IStatisticPlugin;

import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ControllingManager;
import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@Log4j2

@PluginImplementation
public class ActivityByUserPlugin implements IStatisticPlugin {

    @Getter
    private String title = "intranda_statistics_sudan_memory_activity_by_user";
    @Getter
    private PluginType type = PluginType.Statistics;

    @Getter
    @Setter
    private String filter;
    @Getter
    @Setter
    private Date startDate;
    @Getter
    @Setter
    private Date endDate;

    private String startDateText;
    private String endDateText;
    @Getter
    @Setter
    private Date startDateDate;
    @Getter
    @Setter
    private Date endDateDate;

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    @Getter
    @Setter
    private List<Map<String, String>> resultListDetails;
    @Getter
    @Setter
    private List<Map<String, String>> resultListOverview;
    @Getter
    @Setter
    private String timeRange = "%Y-%m";
    private List<String> headerOrderOverview = new ArrayList<>();
    private List<String> headerOrderDetails = new ArrayList<>();
    @Getter
    private List<SelectItem> userNames;
    @Getter
    @Setter
    private String userName;

    private static final String usergroups =
            "'Editing Arabic metadata', 'Editing English metadata', 'Metadata QA', 'Proof Reading Arabic metadata', 'PUB_Final metadata QA Arabic', 'PUB_Final metadata QA English', 'PUB_Layout Wizzard', 'PUB_Topic Tagging', 'Translation of Arabic content to English', 'Translation of English content to Arabic', 'Arabic metadata quality check'";

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

        try {
            userNames = generateUserNames();
        } catch (SQLException e) {
            log.error(e);
        }
    }

    private List<SelectItem> generateUserNames() throws SQLException {
        List<SelectItem> itemList = new ArrayList<>();
        StringBuilder userNameQuery = new StringBuilder();

        userNameQuery.append("SELECT  ");
        userNameQuery.append("DISTINCT (s.BearbeitungsBenutzerID) as id, CONCAT(u.Nachname, ', ', u.Vorname) AS name ");
        userNameQuery.append("FROM ");
        userNameQuery.append("schritte s ");
        userNameQuery.append("INNER JOIN ");
        userNameQuery.append("benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID ");
        userNameQuery.append("WHERE ");
        userNameQuery.append("s.Bearbeitungsstatus = 3 ");
        userNameQuery.append("AND u.IstAktiv = true AND isVisible is NULL ");
        userNameQuery.append("AND s.titel IN (");
        userNameQuery.append(usergroups);
        userNameQuery.append(") ");
        userNameQuery.append("ORDER BY name; ");

        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            itemList = new QueryRunner().query(connection, userNameQuery.toString(), resultSetHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
        return itemList;
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
    plugin_statistics_sudan_timeRange,
    SUM(plugin_statistics_sudan_titleCount) AS plugin_statistics_sudan_titleCount,
    SUM(plugin_statistics_sudan_titlearabicCount) AS plugin_statistics_sudan_titlearabicCount,
    SUM(plugin_statistics_sudan_descriptionCount) AS plugin_statistics_sudan_descriptionCount,
    SUM(plugin_statistics_sudan_descriptionarabicCount) AS plugin_statistics_sudan_descriptionarabicCount,
    COUNT(plugin_statistics_sudan_workflowTitle) AS plugin_statistics_sudan_workflowTitleCount,
    plugin_statistics_sudan_workflowTitle,
    plugin_statistics_sudan_userName
    FROM
    (SELECT
        DATE_FORMAT(s.BearbeitungsEnde, '%Y-%m-%d') AS plugin_statistics_sudan_timeRange,
            WORDCOUNT(m1.value) AS plugin_statistics_sudan_titleCount,
            WORDCOUNT(m2.value) AS plugin_statistics_sudan_titlearabicCount,
            WORDCOUNT(m3.value) AS plugin_statistics_sudan_descriptionCount,
            WORDCOUNT(m4.value) AS plugin_statistics_sudan_descriptionarabicCount,
            s.Titel AS plugin_statistics_sudan_workflowTitle,
            CONCAT(u.Nachname, ', ', u.Vorname) AS plugin_statistics_sudan_userName
    FROM
        schritte s
    LEFT JOIN metadata m1 ON m1.processid = s.ProzesseID
        AND m1.name = 'TitleDocMain'
    LEFT JOIN metadata m2 ON s.ProzesseID = m2.processid
        AND m2.name = 'TitleDocMainArabic'
    LEFT JOIN metadata m3 ON s.ProzesseID = m3.processid
        AND m3.name = 'ContentDescription'
    LEFT JOIN metadata m4 ON s.ProzesseID = m4.processid
        AND m4.name = 'ContentDescriptionArabic'
    LEFT JOIN benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID
    WHERE
        s.typMetadaten = TRUE
            AND s.Bearbeitungsstatus = 3
            AND s.titel IN ('Translation of Arabic content to English' , 'Translation of English content to Arabic', 'Editing English metadata', 'Proof Reading Arabic metadata', 'Arabic metadata quality check', 'Transcribing English Captions')
            AND s.BearbeitungsBenutzerID = 28
            AND s.BearbeitungsEnde BETWEEN '2020-12-01' AND '2020-12-31') a
    GROUP BY plugin_statistics_sudan_timeRange , plugin_statistics_sudan_userName , plugin_statistics_sudan_workflowTitle;


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
            startDateText = dateFormat.format(startDateDate);
        }
        if (endDateDate != null) {
            endDateText = dateFormat.format(endDateDate);
        }
        resultListDetails = null;
        StringBuilder overview = new StringBuilder();

        overview.append("SELECT  ");
        overview.append("plugin_statistics_sudan_timeRange, ");
        overview.append("SUM(plugin_statistics_sudan_titleCount) AS plugin_statistics_sudan_titleCount, ");
        overview.append("SUM(plugin_statistics_sudan_titlearabicCount) AS plugin_statistics_sudan_titlearabicCount, ");
        overview.append("SUM(plugin_statistics_sudan_descriptionCount) AS plugin_statistics_sudan_descriptionCount, ");
        overview.append("SUM(plugin_statistics_sudan_descriptionarabicCount) AS plugin_statistics_sudan_descriptionarabicCount, ");
        overview.append("COUNT(plugin_statistics_sudan_workflowTitle) AS plugin_statistics_sudan_workflowTitleCount, ");
        overview.append("plugin_statistics_sudan_workflowTitle, ");
        overview.append("plugin_statistics_sudan_userName ");
        overview.append("FROM ");

        overview.append("(SELECT ");
        overview.append("DATE_FORMAT(s.BearbeitungsEnde, ");
        overview.append(timeRange);
        overview.append(") AS plugin_statistics_sudan_timeRange, ");
        overview.append("WORDCOUNT(m1.value) AS plugin_statistics_sudan_titleCount, ");
        overview.append("WORDCOUNT(m2.value) AS plugin_statistics_sudan_titlearabicCount, ");
        overview.append("WORDCOUNT(m3.value) AS plugin_statistics_sudan_descriptionCount, ");
        overview.append("WORDCOUNT(m4.value) AS plugin_statistics_sudan_descriptionarabicCount, ");
        overview.append("s.Titel AS plugin_statistics_sudan_workflowTitle, ");
        overview.append("CONCAT(u.Nachname, ', ', u.Vorname) AS plugin_statistics_sudan_userName ");

        overview.append("FROM ");
        overview.append("schritte s ");
        overview.append("    LEFT JOIN (select processid, min(value) as value from metadata where name = 'TitleDocMain' group by processid ) as m1 ON m1.processid = s.ProzesseID ");
        overview.append("    LEFT JOIN (select processid, min(value) as value from metadata where name = 'TitleDocMainArabic' group by processid ) as m2 ON m2.processid = s.ProzesseID ");
        overview.append("    LEFT JOIN (select processid, min(value) as value from metadata where name = 'ContentDescription' group by processid ) as m3 ON m3.processid = s.ProzesseID ");
        overview.append("    LEFT JOIN (select processid, min(value) as value from metadata where name = 'ContentDescriptionArabic' group by processid ) as m4 ON m4.processid = s.ProzesseID ");
        overview.append("    LEFT JOIN benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID ");
        overview.append("    WHERE ");
        overview.append("        s.Bearbeitungsstatus = 3 ");
        overview.append("    AND s.titel in (");
        overview.append(usergroups);
        overview.append(") ");

        if (StringUtils.isNotBlank(userName)) {
            overview.append("AND s.BearbeitungsBenutzerID = ");
            overview.append(userName);
            overview.append(" ");
        }

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

        overview.append(") a ");

        overview.append("GROUP BY plugin_statistics_sudan_timeRange, plugin_statistics_sudan_userName, plugin_statistics_sudan_workflowTitle; ");

        resultListOverview = ControllingManager.getResultsAsMaps(overview.toString());

    }

    public void calculateDetailData() {
        if (startDateDate != null) {
            startDateText = dateFormat.format(startDateDate);
        }
        if (endDateDate != null) {
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
        details.append("schritte s ");

        details.append("    LEFT JOIN (select processid, min(value) as value from metadata where name = 'TitleDocMain' group by processid ) as m1 ON m1.processid = s.ProzesseID ");
        details.append("    LEFT JOIN (select processid, min(value) as value from metadata where name = 'TitleDocMainArabic' group by processid ) as m2 ON m2.processid = s.ProzesseID ");
        details.append("    LEFT JOIN (select processid, min(value) as value from metadata where name = 'ContentDescription' group by processid ) as m3 ON m3.processid = s.ProzesseID ");
        details.append("    LEFT JOIN (select processid, min(value) as value from metadata where name = 'ContentDescriptionArabic' group by processid ) as m4 ON m4.processid = s.ProzesseID ");

        details.append("    LEFT JOIN prozesse p ON s.ProzesseID = p.ProzesseID ");
        details.append("    LEFT JOIN benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID ");
        details.append("WHERE ");
        details.append("  s.titel in (");
        details.append(usergroups);
        details.append(") ");
        details.append("    AND s.Bearbeitungsstatus = 3 ");

        if (StringUtils.isNotBlank(userName)) {
            details.append("AND s.BearbeitungsBenutzerID = ");
            details.append(userName);
            details.append(" ");
        }

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

    public void resetStatistics() {
        resultListOverview = null;
        resultListDetails = null;
        startDateDate = null;
        endDateDate = null;
        startDateText = null;
        endDateText = null;
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
                String val = result.get(headerName);
                if (StringUtils.isNumeric(val)) {
                    resultRow.createCell(columnCounter, CellType.NUMERIC).setCellValue(Integer.parseInt(val));
                } else {
                    resultRow.createCell(columnCounter).setCellValue(val);

                }

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

    private static ResultSetHandler<List<SelectItem>> resultSetHandler = new ResultSetHandler<List<SelectItem>>() {
        @Override
        public List<SelectItem> handle(ResultSet rs) throws SQLException {
            List<SelectItem> answer = new ArrayList<>();
            answer.add(new SelectItem("", ""));
            while (rs.next()) {
                answer.add(new SelectItem(rs.getString("id"), rs.getString("name")));
            }
            return answer;
        }
    };

    @Override
    public String getData() {
        return null;
    }
}
