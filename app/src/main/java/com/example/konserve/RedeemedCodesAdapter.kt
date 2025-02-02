import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konserve.R

class RedeemedCodesAdapter(private var redeemedCodes: MutableList<Pair<String, Int>>) :
    RecyclerView.Adapter<RedeemedCodesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rewardDescription: TextView = view.findViewById(R.id.rewardDescription)
        val rewardPoints: TextView = view.findViewById(R.id.rewardPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_redeemed_code, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (code, points) = redeemedCodes[position]
        holder.rewardDescription.text = "Code: $code"
        holder.rewardPoints.text = "+$points pts"
    }

    override fun getItemCount() = redeemedCodes.size

    fun updateData(newData: List<Pair<String, Int>>) {
        redeemedCodes.clear()
        redeemedCodes.addAll(newData)
        notifyDataSetChanged()
    }
}
