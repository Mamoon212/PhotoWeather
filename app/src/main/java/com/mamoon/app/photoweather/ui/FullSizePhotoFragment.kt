package com.mamoon.app.photoweather.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.mamoon.app.photoweather.R
import com.mamoon.app.photoweather.databinding.FragmentFullSizePhotoBinding


class FullSizePhotoFragment : Fragment() {
    private lateinit var binding: FragmentFullSizePhotoBinding
    private lateinit var mainActivityViewModel: MainActivityViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_full_size_photo, container, false)
        activity?.let {
            mainActivityViewModel = ViewModelProvider(it).get(MainActivityViewModel::class.java)
        }
        binding.lifecycleOwner = this
        binding.viewModel = mainActivityViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainActivityViewModel.photo.observe(viewLifecycleOwner, Observer {
            it?.let {
                activity?.let { activity ->
                    Glide.with(activity)
                        .load(it)
                        .into(binding.fullSizeIV)

                }
                binding.shareButton.isClickable = true
            }
        })

        mainActivityViewModel.reOpenPhoto.observe(viewLifecycleOwner, Observer {
            it?.let { uri ->
                activity?.let { activity ->
                    Glide.with(activity)
                        .load(uri)
                        .into(binding.fullSizeIV)
                }
                binding.shareButton.isClickable = true
            }
        })
    }


}
