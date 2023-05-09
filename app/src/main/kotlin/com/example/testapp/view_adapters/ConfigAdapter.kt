package com.example.testapp.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.testapp.R
import com.example.testapp.configs.DeviceConfig

class ConfigAdapter(private val mList: List<DeviceConfig>) : RecyclerView.Adapter<ConfigAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.config_names_tmpl, parent, false)
        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = mList[position]
        // sets the text to the button from our itemHolder class
        holder.b.text = config.general.name
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val b: Button = itemView.findViewById(R.id.button)
    }
}
