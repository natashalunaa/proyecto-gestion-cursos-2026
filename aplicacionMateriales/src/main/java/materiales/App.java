
package materiales;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.flowable.task.api.Task;

public class App extends JFrame {

    private FlowableService flowable;

    private final JTextField campoUsuario = new JTextField(15);

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Tarea", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modelo);

    public App() {
        setTitle("Gestión de materiales - Flowable");
        setSize(850, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel superior = new JPanel(new FlowLayout());
        superior.add(new JLabel("Usuario administrativo:"));
        superior.add(campoUsuario);

        JButton botonConsultar = new JButton("Consultar tareas");
        superior.add(botonConsultar);
        add(superior, BorderLayout.NORTH);

        tabla.setSelectionMode(
                javax.swing.ListSelectionModel.SINGLE_SELECTION
        );

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel inferior = new JPanel(new GridLayout(1, 2));

        JButton botonReclamar = new JButton("Reclamar tarea");
        JButton botonCompletar = new JButton("Completar tarea");

        inferior.add(botonReclamar);
        inferior.add(botonCompletar);

        add(inferior, BorderLayout.SOUTH);

        botonConsultar.addActionListener(e -> consultarTareas());
        botonReclamar.addActionListener(e -> reclamarTarea());
        botonCompletar.addActionListener(e -> completarTarea());
    }

    private String obtenerUsuario() {
        String usuario = campoUsuario.getText().trim();

        if (usuario.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Ingresá un usuario administrativo."
            );
            return null;
        }

        return usuario;
    }

    private FlowableService obtenerServicio() {
        if (flowable == null) {
            flowable = new FlowableService();
        }
        return flowable;
    }

    private void consultarTareas() {
        String usuario = obtenerUsuario();
        if (usuario == null) return;

        try {
            modelo.setRowCount(0);

            FlowableService servicio = obtenerServicio();

            List<Task> asignadas =
                    servicio.listarTareasAsignadas(usuario);

            List<Task> disponibles =
                    servicio.listarTareasDisponibles(usuario);

            for (Task tarea : asignadas) {
                modelo.addRow(new Object[]{
                        tarea.getId(),
                        tarea.getName(),
                        "Asignada"
                });
            }

            for (Task tarea : disponibles) {
                if (tarea.getAssignee() == null) {
                    modelo.addRow(new Object[]{
                            tarea.getId(),
                            tarea.getName(),
                            "Disponible"
                    });
                }
            }

            if (modelo.getRowCount() == 0) {
                JOptionPane.showMessageDialog(
                        this, "No se encontraron tareas."
                );
            }

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private String obtenerTareaSeleccionada() {
        int fila = tabla.getSelectedRow();

        if (fila == -1) {
            JOptionPane.showMessageDialog(
                    this, "Seleccioná una tarea de la tabla."
            );
            return null;
        }

        int filaModelo = tabla.convertRowIndexToModel(fila);

        return modelo.getValueAt(filaModelo, 0).toString();
    }

    private void reclamarTarea() {
        String usuario = obtenerUsuario();
        if (usuario == null) return;

        String tareaId = obtenerTareaSeleccionada();
        if (tareaId == null) return;

        try {
            obtenerServicio().reclamarTarea(tareaId, usuario);

            JOptionPane.showMessageDialog(
                    this, "Tarea reclamada correctamente."
            );

            consultarTareas();

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void completarTarea() {
        String usuario = obtenerUsuario();
        if (usuario == null) return;

        String tareaId = obtenerTareaSeleccionada();
        if (tareaId == null) return;

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmás que terminaste de preparar los materiales?",
                "Completar tarea",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) return;

        try {
            obtenerServicio().completarTarea(tareaId, usuario);

            JOptionPane.showMessageDialog(
                    this, "Tarea completada correctamente."
            );

            consultarTareas();

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(
                this,
                "Ocurrió un error:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void dispose() {
        if (flowable != null) {
            flowable.close();
        }
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            App ventana = new App();
            ventana.setVisible(true);
        });
    }
}
