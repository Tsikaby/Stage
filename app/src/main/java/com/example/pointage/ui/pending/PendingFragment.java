package com.example.pointage.ui.pending;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;

public class PendingFragment extends Fragment {

    private PendingViewModel viewModel;
    private PendingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recycler = view.findViewById(R.id.recyclerPending);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PendingAdapter();
        recycler.setAdapter(adapter);

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = view.findViewById(R.id.swipeRefreshPending);
        View tvEmpty = view.findViewById(R.id.tvEmpty);

        try {
            viewModel = new ViewModelProvider(this).get(PendingViewModel.class);
        } catch (Exception e) {
            // ignore
        }

        if (viewModel != null) {
            viewModel.getPending().observe(getViewLifecycleOwner(), items -> {
                adapter.setItems(items);
                if (swipe != null && swipe.isRefreshing()) {
                    swipe.setRefreshing(false);
                }
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }

        if (swipe != null) {
            swipe.setOnRefreshListener(() -> {
                if (viewModel != null) viewModel.loadPending();
            });
        }
    }
}
