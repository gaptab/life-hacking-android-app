/* 
* This file is part of Life Hacking
* 
* Life Hacking is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Life Hacking is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Life Hacking.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.samco.trackandgraph.databinding.FaqPageBinding

class FAQFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FaqPageBinding.inflate(inflater, container, false)
        val navController = container?.findNavController()
        binding.faq1.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq1()) }
        binding.faq2.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq2()) }
        binding.faq3.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq3()) }
        binding.faq4.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq4()) }
        binding.faq5.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq5()) }
        binding.faq6.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq6()) }
        binding.faq7.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq7()) }
        return binding.root
    }
}