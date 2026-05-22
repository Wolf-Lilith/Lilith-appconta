package com.joaomartins.lilith

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.joaomartins.lilith.databinding.FragmentListBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ListAccountFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()
    private var showPaid = false

    private val viewModel: AccountViewModel by viewModels {
        AccountViewModelFactory((requireActivity().application as LilithApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapterOverdue = AccountAdapter(onPaidClick = { viewModel.update(it.copy(isPaid = !it.isPaid)) }, onLongClick = { showDeleteDialog(it) })
        val adapterFirst = AccountAdapter(onPaidClick = { viewModel.update(it.copy(isPaid = !it.isPaid)) }, onLongClick = { showDeleteDialog(it) })
        val adapterSecond = AccountAdapter(onPaidClick = { viewModel.update(it.copy(isPaid = !it.isPaid)) }, onLongClick = { showDeleteDialog(it) })

        setupRecyclers(adapterOverdue, adapterFirst, adapterSecond)
        updateMonthDisplay()
        updateVisibilityIcon()

        viewModel.allAccounts.observe(viewLifecycleOwner) { accounts ->
            updateUIWithAccounts(accounts)
        }

        binding.btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateMonthDisplay()
            viewModel.allAccounts.value?.let { updateUIWithAccounts(it) }
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateMonthDisplay()
            viewModel.allAccounts.value?.let { updateUIWithAccounts(it) }
        }

        binding.btnToggleVisibility.setOnClickListener {
            showPaid = !showPaid
            updateVisibilityIcon()
            viewModel.allAccounts.value?.let { updateUIWithAccounts(it) }
        }

        binding.btnExportPng.setOnClickListener {
            shareRelatorioDeElite()
        }
    }

    private fun updateVisibilityIcon() {
        binding.btnToggleVisibility.setImageResource(if (showPaid) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_close_clear_cancel)
    }

    private fun updateUIWithAccounts(accounts: List<Account>) {
        val selectedMonth = calendar.get(Calendar.MONTH)
        val selectedYear = calendar.get(Calendar.YEAR)
        val itemCalendar = Calendar.getInstance()

        val overdue = accounts.filter {
            itemCalendar.timeInMillis = it.dueDate
            val itemMonth = itemCalendar.get(Calendar.MONTH)
            val itemYear = itemCalendar.get(Calendar.YEAR)
            (itemYear < selectedYear || (itemYear == selectedYear && itemMonth < selectedMonth)) && !it.isPaid && !it.isRevenue
        }
        (binding.rvOverdue.adapter as? AccountAdapter)?.submitList(overdue)
        binding.titleOverdue.visibility = if (overdue.isEmpty()) View.GONE else View.VISIBLE

        val monthAccounts = accounts.filter {
            itemCalendar.timeInMillis = it.dueDate
            itemCalendar.get(Calendar.MONTH) == selectedMonth && itemCalendar.get(Calendar.YEAR) == selectedYear
        }

        val visibleMonthAccounts = if (showPaid) monthAccounts else monthAccounts.filter { !it.isPaid }

        val firstFortnight = visibleMonthAccounts.filter {
            itemCalendar.timeInMillis = it.dueDate
            itemCalendar.get(Calendar.DAY_OF_MONTH) <= 15
        }
        (binding.rvFirstFortnight.adapter as? AccountAdapter)?.submitList(firstFortnight)

        val secondFortnight = visibleMonthAccounts.filter {
            itemCalendar.timeInMillis = it.dueDate
            itemCalendar.get(Calendar.DAY_OF_MONTH) > 15
        }
        (binding.rvSecondFortnight.adapter as? AccountAdapter)?.submitList(secondFortnight)

        calculateSummaryForUI(overdue, monthAccounts)
    }

    private fun calculateSummaryForUI(overdue: List<Account>, monthAccounts: List<Account>) {
        var expensesPaid = 0.0
        var expensesPending = 0.0
        var totalRevenues = 0.0

        overdue.forEach { expensesPending += it.value }
        monthAccounts.forEach {
            if (it.isRevenue) totalRevenues += it.value
            else {
                if (it.isPaid) expensesPaid += it.value else expensesPending += it.value
            }
        }

        val totalExpenses = expensesPaid + expensesPending
        val balance = totalRevenues - totalExpenses

        binding.txtTotalGeneral.text = String.format("R$ %.2f", totalExpenses)
        binding.txtTotalPaid.text = String.format("R$ %.2f", expensesPaid)
        binding.txtTotalPending.text = String.format("R$ %.2f", expensesPending)
        binding.txtBalance.text = String.format("R$ %.2f", balance)
    }

    fun shareRelatorioDeElite() {
        try {
            // Infla o layout do relatório
            val reportView = LayoutInflater.from(requireContext()).inflate(R.layout.report_account_table, null)
            
            // Preenche os dados
            preencherRelatorio(reportView)
            
            // Converte para Bitmap
            val bitmap = layoutToBitmap(reportView)

            // Salva em cache
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "relatorio_lilith.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // Compartilha
            val contentUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Erro ao gerar relatório: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun preencherRelatorio(reportView: View) {
        val container = reportView.findViewById<LinearLayout>(R.id.report_rows_container)
        val txtTotalGeneral = reportView.findViewById<TextView>(R.id.report_txt_total_general)
        val txtTotalPaid = reportView.findViewById<TextView>(R.id.report_txt_total_paid)
        val txtTotalPending = reportView.findViewById<TextView>(R.id.report_txt_total_pending)
        val txtBalance = reportView.findViewById<TextView>(R.id.report_txt_balance)
        val txtTitle = reportView.findViewById<TextView>(R.id.report_title)

        val accounts = viewModel.allAccounts.value ?: emptyList()
        val itemCalendar = Calendar.getInstance()
        val selectedMonth = calendar.get(Calendar.MONTH)
        val selectedYear = calendar.get(Calendar.YEAR)

        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
        txtTitle.text = "RELATÓRIO: ${monthName?.uppercase()} $selectedYear"

        var expPaid = 0.0
        var expPending = 0.0
        var revTotal = 0.0

        val monthAccounts = accounts.filter {
            itemCalendar.timeInMillis = it.dueDate
            itemCalendar.get(Calendar.MONTH) == selectedMonth && itemCalendar.get(Calendar.YEAR) == selectedYear
        }.sortedBy { it.dueDate }

        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())

        if (monthAccounts.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "Nenhuma conta lançada para este mês."
                setTextColor(Color.GRAY)
                setPadding(0, 20, 0, 20)
            }
            container.addView(emptyText)
        }

        monthAccounts.forEach { account ->
            val rowText = TextView(requireContext()).apply {
                val status = if (account.isPaid) "PAGO" else "PENDENTE"
                val prefix = if (account.isRevenue) "[REC]" else "[DESP]"
                text = "$prefix ${sdf.format(Date(account.dueDate))} - ${account.description}: R$ ${String.format("%.2f", account.value)} ($status)"
                
                // Cores para o relatório (fundo branco)
                if (account.isRevenue) {
                    setTextColor(Color.BLUE)
                } else {
                    setTextColor(if (account.isPaid) Color.parseColor("#2E7D32") else Color.RED)
                }
                textSize = 14f
                setPadding(0, 8, 0, 8)
            }
            container.addView(rowText)

            if (account.isRevenue) revTotal += account.value
            else {
                if (account.isPaid) expPaid += account.value else expPending += account.value
            }
        }

        txtTotalGeneral.text = "Total Despesas: R$ ${String.format("%.2f", expPaid + expPending)}"
        txtTotalPaid.text = "Total Pago: R$ ${String.format("%.2f", expPaid)}"
        txtTotalPending.text = "Total Pendente: R$ ${String.format("%.2f", expPending)}"
        
        val finalBalance = revTotal - (expPaid + expPending)
        txtBalance.text = "Saldo Final Previsto: R$ ${String.format("%.2f", finalBalance)}"
        txtBalance.setTextColor(if (finalBalance >= 0) Color.BLUE else Color.RED)
    }

    private fun layoutToBitmap(view: View): Bitmap {
        // Define uma largura padrão para o relatório (ex: 1080px para boa qualidade)
        val width = 1080
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        
        view.measure(widthSpec, heightSpec)
        val height = view.measuredHeight
        
        view.layout(0, 0, width, height)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // Garante fundo branco
        view.draw(canvas)
        return bitmap
    }

    private fun setupRecyclers(overdue: AccountAdapter, first: AccountAdapter, second: AccountAdapter) {
        binding.rvOverdue.layoutManager = LinearLayoutManager(requireContext()); binding.rvOverdue.adapter = overdue
        binding.rvFirstFortnight.layoutManager = LinearLayoutManager(requireContext()); binding.rvFirstFortnight.adapter = first
        binding.rvSecondFortnight.layoutManager = LinearLayoutManager(requireContext()); binding.rvSecondFortnight.adapter = second
    }

    private fun updateMonthDisplay() {
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
        binding.txtCurrentMonthYear.text = "${monthName?.replaceFirstChar { it.uppercase() }} ${calendar.get(Calendar.YEAR)}"
    }

    private fun showDeleteDialog(account: Account) {
        val builder = AlertDialog.Builder(requireContext())
        if (account.totalParcels > 1) {
            builder.setTitle("Excluir Parcelas").setItems(arrayOf("Apenas esta", "Esta e próximas", "Todas")) { _, i ->
                when(i) {
                    0 -> viewModel.delete(account)
                    1 -> viewModel.deleteFromCurrentParcel(account)
                    2 -> viewModel.deleteAllParcels(account)
                }
            }.show()
        } else {
            builder.setTitle("Excluir").setMessage("Apagar '${account.description}'?").setPositiveButton("Sim") { _, _ -> viewModel.delete(account) }.show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}