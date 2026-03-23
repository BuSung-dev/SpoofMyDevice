package com.devicespooflab.hooks.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.MainActivity;
import com.devicespooflab.hooks.R;
import com.devicespooflab.hooks.data.AppSettingsStore;
import com.devicespooflab.hooks.databinding.FragmentAppSettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

public class AppSettingsFragment extends Fragment {

    private FragmentAppSettingsBinding binding;
    private boolean applying;
    private boolean systemColorsToggleFromRow;
    private boolean resolutionToggleFromRow;
    private List<Option> themeOptions;
    private List<Option> languageOptions;
    private List<Option> colorStyleOptions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupOptionLists();
        setupActions();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFromHost();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void refreshFromHost() {
        if (binding == null || !(requireActivity() instanceof MainActivity)) {
            return;
        }

        MainActivity activity = (MainActivity) requireActivity();
        boolean systemColorsEnabled = AppSettingsStore.isSystemColorEnabled(requireContext());
        applying = true;
        binding.themeSummary.setText(getString(
            R.string.settings_theme_summary,
            findLabel(themeOptions, AppSettingsStore.getThemeMode(requireContext()))
        ));
        binding.languageSummary.setText(getString(
            R.string.settings_language_summary,
            findLabel(languageOptions, AppSettingsStore.getLanguageMode(requireContext()))
        ));
        binding.colorStyleSummary.setText(getString(
            R.string.settings_color_style_summary,
            findLabel(colorStyleOptions, AppSettingsStore.getColorStyle(requireContext()))
        ));
        binding.systemColorsSwitch.setChecked(systemColorsEnabled);
        binding.colorStyleRow.setVisibility(systemColorsEnabled ? View.GONE : View.VISIBLE);
        binding.resolutionSwitch.setChecked(activity.isScreenMetricsSpoofEnabled());
        applying = false;
    }

    private void setupOptionLists() {
        themeOptions = Arrays.asList(
            new Option(AppSettingsStore.THEME_SYSTEM, getString(R.string.theme_system)),
            new Option(AppSettingsStore.THEME_LIGHT, getString(R.string.theme_light)),
            new Option(AppSettingsStore.THEME_DARK, getString(R.string.theme_dark))
        );
        languageOptions = Arrays.asList(
            new Option(AppSettingsStore.LANGUAGE_DEFAULT, getString(R.string.language_default)),
            new Option(AppSettingsStore.LANGUAGE_ENGLISH, getString(R.string.language_english)),
            new Option(AppSettingsStore.LANGUAGE_KOREAN, getString(R.string.language_korean)),
            new Option(AppSettingsStore.LANGUAGE_JAPANESE, getString(R.string.language_japanese))
        );
        colorStyleOptions = Arrays.asList(
            new Option(AppSettingsStore.COLOR_STYLE_MINT, getString(R.string.color_style_mint)),
            new Option(AppSettingsStore.COLOR_STYLE_BLUE, getString(R.string.color_style_blue)),
            new Option(AppSettingsStore.COLOR_STYLE_ROSE, getString(R.string.color_style_rose)),
            new Option(AppSettingsStore.COLOR_STYLE_AMBER, getString(R.string.color_style_amber))
        );
    }

    private void setupActions() {
        binding.openRealInfoRow.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openRealInfo();
            }
        });

        binding.themeRow.setOnClickListener(v -> showThemeDialog());
        binding.languageRow.setOnClickListener(v -> showLanguageDialog());
        binding.colorStyleRow.setOnClickListener(v -> showColorStyleDialog());
        binding.systemColorsRow.setOnClickListener(v -> {
            systemColorsToggleFromRow = true;
            binding.systemColorsSwitch.toggle();
        });

        binding.systemColorsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean userInitiated = buttonView.isPressed() || systemColorsToggleFromRow;
            systemColorsToggleFromRow = false;
            if (applying || !userInitiated) {
                return;
            }
            AppSettingsStore.setSystemColorEnabled(requireContext(), isChecked);
            refreshFromHost();
            buttonView.post(this::recreateHostSafely);
        });

        binding.resolutionRow.setOnClickListener(v -> {
            resolutionToggleFromRow = true;
            binding.resolutionSwitch.toggle();
        });
        binding.resolutionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean userInitiated = buttonView.isPressed() || resolutionToggleFromRow;
            resolutionToggleFromRow = false;
            if (applying || !userInitiated || !(requireActivity() instanceof MainActivity)) {
                return;
            }
            MainActivity activity = (MainActivity) requireActivity();
            boolean updated = activity.updateScreenMetricsSpoofEnabled(isChecked);
            if (!updated) {
                applying = true;
                binding.resolutionSwitch.setChecked(!isChecked);
                applying = false;
            }
        });
    }

    private void showThemeDialog() {
        if (binding == null) {
            return;
        }

        CharSequence[] items = labels(themeOptions);
        int checkedIndex = findIndex(themeOptions, AppSettingsStore.getThemeMode(requireContext()));
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_choose_theme)
            .setSingleChoiceItems(items, checkedIndex, (dialog, which) -> {
                if (which >= 0 && which < themeOptions.size()) {
                    String selectedMode = themeOptions.get(which).id;
                    if (!selectedMode.equals(AppSettingsStore.getThemeMode(requireContext()))) {
                        dialog.dismiss();
                        AppSettingsStore.setThemeMode(requireContext(), selectedMode);
                        return;
                    }
                    refreshFromHost();
                }
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void showColorStyleDialog() {
        if (binding == null) {
            return;
        }

        CharSequence[] items = labels(colorStyleOptions);
        int checkedIndex = findIndex(colorStyleOptions, AppSettingsStore.getColorStyle(requireContext()));
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_choose_color_style)
            .setSingleChoiceItems(items, checkedIndex, (dialog, which) -> {
                if (which >= 0 && which < colorStyleOptions.size()) {
                    String selectedStyle = colorStyleOptions.get(which).id;
                    if (!selectedStyle.equals(AppSettingsStore.getColorStyle(requireContext()))) {
                        AppSettingsStore.setColorStyle(requireContext(), selectedStyle);
                        refreshFromHost();
                        dialog.dismiss();
                        recreateHostSafely();
                        return;
                    }
                }
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void showLanguageDialog() {
        if (binding == null) {
            return;
        }

        CharSequence[] items = labels(languageOptions);
        int checkedIndex = findIndex(languageOptions, AppSettingsStore.getLanguageMode(requireContext()));
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_choose_language)
            .setSingleChoiceItems(items, checkedIndex, (dialog, which) -> {
                if (which >= 0 && which < languageOptions.size()) {
                    String selectedMode = languageOptions.get(which).id;
                    if (!selectedMode.equals(AppSettingsStore.getLanguageMode(requireContext()))) {
                        dialog.dismiss();
                        AppSettingsStore.setLanguageMode(requireContext(), selectedMode);
                        return;
                    }
                    refreshFromHost();
                }
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private CharSequence[] labels(List<Option> options) {
        CharSequence[] labels = new CharSequence[options.size()];
        for (int i = 0; i < options.size(); i++) {
            labels[i] = options.get(i).label;
        }
        return labels;
    }

    private int findIndex(List<Option> options, String id) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id.equals(id)) {
                return i;
            }
        }
        return 0;
    }

    private String findLabel(List<Option> options, String id) {
        for (Option option : options) {
            if (option.id.equals(id)) {
                return option.label;
            }
        }
        return options.isEmpty() ? "" : options.get(0).label;
    }

    private void recreateHostSafely() {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        if (requireActivity().isFinishing() || requireActivity().isDestroyed()) {
            return;
        }
        requireActivity().recreate();
    }

    private static final class Option {
        private final String id;
        private final String label;

        private Option(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }
}
