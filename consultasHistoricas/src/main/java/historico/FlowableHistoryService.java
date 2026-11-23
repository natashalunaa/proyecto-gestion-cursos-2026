
package historico;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;

public class FlowableHistoryService implements AutoCloseable {

    private final ProcessEngine processEngine;
    private final HistoryService historyService;

    public FlowableHistoryService() {

        processEngine = ProcessEngineConfiguration
                .createStandaloneProcessEngineConfiguration()
                .setJdbcDriver("org.postgresql.Driver")
                .setJdbcUrl(
                        "jdbc:postgresql://127.0.0.1:5433/flowable"
                )
                .setJdbcUsername("flowable")
                .setJdbcPassword("flow10")
                .setDatabaseSchemaUpdate(
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE
                )
                .setAsyncExecutorActivate(false)
                .buildProcessEngine();

        historyService = processEngine.getHistoryService();
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    @Override
    public void close() {
        processEngine.close();
    }
}
