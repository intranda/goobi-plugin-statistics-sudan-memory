package de.intranda.goobi.plugins.statistics;

import org.apache.commons.lang.StringUtils;

import de.sub.goobi.persistence.managers.ControllingManager;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@Data
@PluginImplementation
@EqualsAndHashCode(callSuper = false)
public class ActivityByUserPlugin extends AbstractSudanStatistics {

    private String title = "plugin_statistics_sudan_activity_by_user";

    @Override
    public String getGui() {
        return "/uii/plugin_statistics_sudan_activity_by_user.xhtml";
    }

    @Override
    public void calculate() {
        calculateData();
    }

    /**
     * calculate data from database
     */
    @Override
    protected void calculateData() {

        StringBuilder sb = new StringBuilder();

        // #6. How much activity has been logged for each step, and by what users and groups? What is the average activity time for each step?
        // #Select the average time a task was processed, listed per task type per user, can be limited to a time range

        sb.append("select ");
        sb.append("sum(p.sortHelperImages) as plugin_statistics_stanford_number_images, ");
        sb.append("count(s.SchritteID) as plugin_statistics_stanford_number_steps, ");
        sb.append("s.titel as plugin_statistics_stanford_step, ");
        sb.append("concat(u.Nachname, ', ', u.Vorname) as plugin_statistics_stanford_step_username, ");
        sb.append("avg(TIMEDIFF(s.BearbeitungsEnde, s.BearbeitungsBeginn)) as plugin_statistics_stanford_step_average_duration ");
        sb.append("from schritte s ");
        sb.append("left join prozesse p on s.ProzesseID = p.ProzesseID ");
        sb.append("left join benutzer u on s.BearbeitungsBenutzerID = u.BenutzerID ");
        sb.append("where s.BearbeitungsStatus = 3 ");
        sb.append("and s.typAutomatisch = false ");

        //        sb.append("and s1.BearbeitungsEnde between '2018-01-01' and '2018-12-31'; ");
        if (StringUtils.isNotBlank(startDateText) && StringUtils.isNotBlank(endDateText)) {
            sb.append("and s.BearbeitungsEnde between '");
            sb.append(startDateText);
            sb.append("' and '");
            sb.append(endDateText);
            sb.append("' ");
        } else if (StringUtils.isNotBlank(startDateText)) {
            sb.append("and s.BearbeitungsEnde >= '");
            sb.append(startDateText);
            sb.append("' ");
        } else if (StringUtils.isNotBlank(endDateText)) {
            sb.append("and s.BearbeitungsEnde <= '");
            sb.append(endDateText);
            sb.append("' ");
        }

        //# and projekte.Titel = "Piano Rolls" - to only query a single project
        if (StringUtils.isNotBlank(project)) {
            sb.append("and projekte.Titel = '");
            sb.append(project);
            sb.append("' ");
        }
        // constrain by Step
        if (StringUtils.isNotBlank(step)) {
            sb.append("AND s1.Titel = '");
            sb.append(step);
            sb.append("' ");
        }

        sb.append("group by s.titel, u.login");

        resultList = ControllingManager.getResultsAsMaps(sb.toString());

    }

}
