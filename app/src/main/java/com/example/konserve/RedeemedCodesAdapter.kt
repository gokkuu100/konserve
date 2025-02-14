import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konserve.R

class RedeemedCodesAdapter(private var redeemedCodesList: MutableList<Pair<String, Int>>) :
    RecyclerView.Adapter<RedeemedCodesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rewardPoints: TextView = view.findViewById(R.id.rewardPoints)
        val rewardDescription: TextView = view.findViewById(R.id.rewardDescription)
        val rewardCode: TextView = view.findViewById(R.id.rewardCode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_redeemed_code, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (code, points) = redeemedCodesList[position]

        holder.rewardPoints.text = "+$points"
        holder.rewardDescription.text = "You earned $points points"
        holder.rewardCode.text = code

        Log.d("Adapter", "Binding: Code=$code, Points=$points")
    }

    override fun getItemCount(): Int = redeemedCodesList.size

    fun updateData(newList: List<Pair<String, Int>>) {
        redeemedCodesList.clear()
        redeemedCodesList.addAll(newList)
        notifyItemRangeInserted(0, newList.size)
    }
}
