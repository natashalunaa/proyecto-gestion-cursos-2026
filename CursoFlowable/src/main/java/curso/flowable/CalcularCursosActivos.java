package curso.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;


//implements JavaDelegate: le indica a Flowable que esta clase puede ejecutarse como una Service Task.
public class CalcularCursosActivos implements JavaDelegate {  

    @Override

    //execute(...): es el método que Flowable ejecutará automáticamente cuando el proceso llegue a la tarea.
    public void execute(DelegateExecution execution) {         

        // Valor de prueba, hasta implementar la consulta real
        int cantidadCursosActivos = 0;

        // Guardamos el resultado como variable del proceso
        execution.setVariable(
            "cantidadCursosActivos", cantidadCursosActivos
        );

        System.out.println(
            "Cantidad de cursos activos: " + cantidadCursosActivos
        );
    }
}
