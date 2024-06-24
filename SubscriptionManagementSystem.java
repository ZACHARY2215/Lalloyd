import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

class Customer {
    private String name;
    private String email;
    private Subscription subscription;

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }
}

class Subscription {
    private String planName;
    private String billingCycle;
    private boolean isRenewal;
    private boolean isPaid;
    private Date subscriptionDate;
    private String paymentMethod;

    public Subscription(String planName, String billingCycle, boolean isRenewal, boolean isPaid, Date subscriptionDate,
            String paymentMethod) {
        this.planName = planName;
        this.billingCycle = billingCycle;
        this.isRenewal = isRenewal;
        this.isPaid = isPaid;
        this.subscriptionDate = subscriptionDate;
        this.paymentMethod = paymentMethod;
    }

    public String getPlanName() {
        return planName;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public boolean isRenewal() {
        return isRenewal;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public Date getSubscriptionDate() {
        return subscriptionDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Date getDueDate() {
        long cycleMillis = 0;
        switch (billingCycle.toLowerCase()) {
            case "monthly":
                cycleMillis = 30L * 24 * 60 * 60 * 1000;
                break;
            case "yearly":
                cycleMillis = 365L * 24 * 60 * 60 * 1000;
                break;
            default:
                cycleMillis = 30L * 24 * 60 * 60 * 1000;
                break;
        }
        return new Date(subscriptionDate.getTime() + cycleMillis);
    }
}

public class SubscriptionManagementSystem extends JFrame {
    private List<Customer> customers = new ArrayList<>();
    private JTextField nameField, emailField, planField;
    private JComboBox<String> cycleComboBox, paymentMethodComboBox;
    private JCheckBox renewalCheckBox, paidCheckBox;
    private JTable subscriptionTable;
    private DefaultTableModel tableModel;
    private TimerPanel timerPanel;
    private JButton addButton, updateButton, deleteButton;

    public SubscriptionManagementSystem() {
        setTitle("Subscription Management System");
        setSize(900, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(7, 2));
        inputPanel.add(new JLabel("Customer Name:"));
        nameField = new JTextField();
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Email:"));
        emailField = new JTextField();
        inputPanel.add(emailField);

        inputPanel.add(new JLabel("Subscription Plan:"));
        planField = new JTextField();
        inputPanel.add(planField);

        inputPanel.add(new JLabel("Billing Cycle:"));
        cycleComboBox = new JComboBox<>(new String[] { "Monthly", "Yearly" });
        inputPanel.add(cycleComboBox);

        inputPanel.add(new JLabel("Payment Method:"));
        paymentMethodComboBox = new JComboBox<>(
                new String[] { "Credit Card", "Debit Card", "PayPal", "Bank Transfer" });
        inputPanel.add(paymentMethodComboBox);

        inputPanel.add(new JLabel("Auto Renewal:"));
        renewalCheckBox = new JCheckBox();
        inputPanel.add(renewalCheckBox);

        inputPanel.add(new JLabel("Payment Status:"));
        paidCheckBox = new JCheckBox();
        inputPanel.add(paidCheckBox);

        add(inputPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add Subscription");
        addButton.addActionListener(new AddButtonListener());
        buttonPanel.add(addButton);

        updateButton = new JButton("Update Subscription");
        updateButton.addActionListener(new UpdateButtonListener());
        buttonPanel.add(updateButton);

        deleteButton = new JButton("Delete Subscription");
        deleteButton.addActionListener(new DeleteButtonListener());
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.CENTER);

        String[] columnNames = { "Name", "Email", "Plan", "Billing Cycle", "Renewal", "Paid", "Payment Method",
                "Subscription Date", "Due Date" };
        tableModel = new DefaultTableModel(columnNames, 0);
        subscriptionTable = new JTable(tableModel);
        subscriptionTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        subscriptionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = subscriptionTable.getSelectedRow();
                if (selectedRow != -1) {
                    nameField.setText((String) tableModel.getValueAt(selectedRow, 0));
                    emailField.setText((String) tableModel.getValueAt(selectedRow, 1));
                    planField.setText((String) tableModel.getValueAt(selectedRow, 2));
                    cycleComboBox.setSelectedItem(tableModel.getValueAt(selectedRow, 3));
                    renewalCheckBox.setSelected((Boolean) tableModel.getValueAt(selectedRow, 4));
                    paidCheckBox.setSelected((Boolean) tableModel.getValueAt(selectedRow, 5));
                    paymentMethodComboBox.setSelectedItem(tableModel.getValueAt(selectedRow, 6));
                }
            }
        });
        add(new JScrollPane(subscriptionTable), BorderLayout.SOUTH);

        timerPanel = new TimerPanel();
        add(new JScrollPane(timerPanel), BorderLayout.EAST);
    }

    private class AddButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                String name = nameField.getText();
                String email = emailField.getText();
                String plan = planField.getText();
                String cycle = (String) cycleComboBox.getSelectedItem();
                String paymentMethod = (String) paymentMethodComboBox.getSelectedItem();
                boolean isRenewal = renewalCheckBox.isSelected();
                boolean isPaid = paidCheckBox.isSelected();

                if (name.isEmpty() || email.isEmpty() || plan.isEmpty() || cycle == null || paymentMethod == null) {
                    throw new IllegalArgumentException("All fields must be filled.");
                }

                Customer customer = new Customer(name, email);
                Subscription subscription = new Subscription(plan, cycle, isRenewal, isPaid, new Date(), paymentMethod);
                customer.setSubscription(subscription);
                customers.add(customer);

                timerPanel.addTimer(subscription);
                writeToFile();
                updateTable();

                JOptionPane.showMessageDialog(null, "Subscription added successfully!");
                clearFields();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class UpdateButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int selectedRow = subscriptionTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null, "Select a subscription to update.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            try {
                String name = nameField.getText();
                String email = emailField.getText();
                String plan = planField.getText();
                String cycle = (String) cycleComboBox.getSelectedItem();
                String paymentMethod = (String) paymentMethodComboBox.getSelectedItem();
                boolean isRenewal = renewalCheckBox.isSelected();
                boolean isPaid = paidCheckBox.isSelected();

                if (name.isEmpty() || email.isEmpty() || plan.isEmpty() || cycle == null || paymentMethod == null) {
                    throw new IllegalArgumentException("All fields must be filled.");
                }

                Customer customer = customers.get(selectedRow);
                Subscription subscription = new Subscription(plan, cycle, isRenewal, isPaid, new Date(), paymentMethod);
                customer.setSubscription(subscription);

                writeToFile();
                updateTable();

                JOptionPane.showMessageDialog(null, "Subscription updated successfully!");
                clearFields();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class DeleteButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int selectedRow = subscriptionTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null, "Select a subscription to delete.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            customers.remove(selectedRow);
            writeToFile();
            updateTable();

            JOptionPane.showMessageDialog(null, "Subscription deleted successfully!");
        }
    }

    private void clearFields() {
        nameField.setText("");
        emailField.setText("");
        planField.setText("");
        cycleComboBox.setSelectedIndex(0);
        paymentMethodComboBox.setSelectedIndex(0);
        renewalCheckBox.setSelected(false);
        paidCheckBox.setSelected(false);
    }

    private void writeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("subscriptions.txt"))) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            for (Customer customer : customers) {
                Subscription sub = customer.getSubscription();
                writer.write("Name: " + customer.getName() + "\n");
                writer.write("Email: " + customer.getEmail() + "\n");
                writer.write("Plan: " + sub.getPlanName() + "\n");
                writer.write("Billing Cycle: " + sub.getBillingCycle() + "\n");
                writer.write("Auto Renewal: " + sub.isRenewal() + "\n");
                writer.write("Payment Status: " + sub.isPaid() + "\n");
                writer.write("Payment Method: " + sub.getPaymentMethod() + "\n");
                writer.write("Subscription Date: " + sdf.format(sub.getSubscriptionDate()) + "\n");
                writer.write("Due Date: " + sdf.format(sub.getDueDate()) + "\n");
                writer.write("-------------------------------\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error writing to file: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        for (Customer customer : customers) {
            Subscription sub = customer.getSubscription();
            Object[] row = {
                    customer.getName(),
                    customer.getEmail(),
                    sub.getPlanName(),
                    sub.getBillingCycle(),
                    sub.isRenewal(),
                    sub.isPaid(),
                    sub.getPaymentMethod(),
                    sdf.format(sub.getSubscriptionDate()),
                    sdf.format(sub.getDueDate())
            };
            tableModel.addRow(row);
        }
    }

    private class TimerPanel extends JPanel {
        private List<JLabel> timerLabels = new ArrayList<>();

        public TimerPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        public void addTimer(Subscription subscription) {
            JLabel timerLabel = new JLabel();
            timerLabels.add(timerLabel);
            add(timerLabel);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    long remainingTime = subscription.getDueDate().getTime() - new Date().getTime();
                    if (remainingTime <= 0) {
                        timerLabel.setText("Subscription for " + subscription.getPlanName() + " is due!");
                        timer.cancel();
                    } else {
                        long days = remainingTime / (1000 * 60 * 60 * 24);
                        long hours = (remainingTime / (1000 * 60 * 60)) % 24;
                        long minutes = (remainingTime / (1000 * 60)) % 60;
                        long seconds = (remainingTime / 1000) % 60;
                        timerLabel.setText("Time until due for " + subscription.getPlanName() + ": " + days + "d "
                                + hours + "h " + minutes + "m " + seconds + "s");
                    }
                }
            }, 0, 1000);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SubscriptionManagementSystem().setVisible(true);
        });
    }
}
