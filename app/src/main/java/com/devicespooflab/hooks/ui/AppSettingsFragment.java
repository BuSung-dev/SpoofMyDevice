package com.devicespooflab.hooks.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.MainActivity;
import com.devicespooflab.hooks.R;
import com.devicespooflab.hooks.SafeModeAppsActivity;
import com.devicespooflab.hooks.data.AppSettingsStore;
import com.devicespooflab.hooks.databinding.FragmentAppSettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AppSettingsFragment extends Fragment {

    private FragmentAppSettingsBinding binding;
    private boolean applying;
    private boolean systemColorsToggleFromRow;
    private boolean resolutionToggleFromRow;
    private List<Option> themeOptions;
    private List<Option> languageOptions;
    private List<Option> colorStyleOptions;
    private final ActivityResultLauncher<Intent> safeModePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
                return;
            }
            if (!(requireActivity() instanceof MainActivity)) {
                return;
            }
            ArrayList<String> packages = result.getData().getStringArrayListExtra(
                SafeModeAppsActivity.EXTRA_RESULT_SELECTED_PACKAGES
            );
            LinkedHashSet<String> selectedPackages = new LinkedHashSet<>();
            if (packages != null) {
                selectedPackages.addAll(packages);
            }
            ((MainActivity) requireActivity()).updateSafeModePackagesAsync(selectedPackages);
        });

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
        binding.presetSourceSummary.setText(AppSettingsStore.getPresetSourceUrl(requireContext()));
        Set<String> safeModePackages = activity.getSafeModePackages();
        if (safeModePackages.isEmpty()) {
            binding.safeModeSummary.setText(R.string.settings_safe_mode_summary_disabled);
        } else {
            binding.safeModeSummary.setText(
                getString(R.string.settings_safe_mode_summary_count, safeModePackages.size())
            );
        }
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

        binding.presetSourceRow.setOnClickListener(v -> showPresetSourceDialog());
        binding.themeRow.setOnClickListener(v -> showThemeDialog());
        binding.languageRow.setOnClickListener(v -> showLanguageDialog());
        binding.colorStyleRow.setOnClickListener(v -> showColorStyleDialog());
        binding.safeModeRow.setOnClickListener(v -> openSafeModePicker());
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

    private void showPresetSourceDialog() {
        if (!(requireActivity() instanceof MainActivity)) {
            return;
        }
        MainActivity activity = (MainActivity) requireActivity();
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_preset_source, null, false);
        RadioGroup radioGroup = dialogView.findViewById(R.id.preset_source_radio_group);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.preset_source_input_layout);
        TextInputEditText input = dialogView.findViewById(R.id.preset_source_input);

        String currentUrl = activity.getPresetSourceUrl();
        boolean usingDefault = AppSettingsStore.DEFAULT_PRESET_SOURCE_URL.equals(currentUrl);
        input.setText(usingDefault ? "" : currentUrl);
        radioGroup.check(usingDefault ? R.id.preset_source_option_default : R.id.preset_source_option_custom);
        updatePresetSourceInputState(inputLayout, input, !usingDefault);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean customSelected = checkedId == R.id.preset_source_option_custom;
            updatePresetSourceInputState(inputLayout, input, customSelected);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_preset_source_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean customSelected = radioGroup.getCheckedRadioButtonId() == R.id.preset_source_option_custom;
            inputLayout.setError(null);
            String nextValue;
            if (customSelected) {
                String customUrl = input.getText() == null ? "" : input.getText().toString().trim();
                if (customUrl.isEmpty()) {
                    inputLayout.setError(getString(R.string.settings_preset_source_required));
                    input.requestFocus();
                    return;
                }
                nextValue = customUrl;
            } else {
                nextValue = AppSettingsStore.DEFAULT_PRESET_SOURCE_URL;
            }

            activity.updatePresetSourceUrl(nextValue);
            refreshFromHost();
            dialog.dismiss();
        });
    }

    private void updatePresetSourceInputState(TextInputLayout inputLayout, TextInputEditText input, boolean enabled) {
        inputLayout.setEnabled(enabled);
        input.setEnabled(enabled);
        input.setFocusable(enabled);
        input.setFocusableInTouchMode(enabled);
        input.setClickable(enabled);
        if (enabled) {
            input.requestFocus();
        } else {
            inputLayout.setError(null);
        }
    }

    private void openSafeModePicker() {
        if (!(requireActivity() instanceof MainActivity)) {
            return;
        }
        MainActivity activity = (MainActivity) requireActivity();
        safeModePickerLauncher.launch(
            SafeModeAppsActivity.createIntent(requireContext(), activity.getSafeModePackages())
        );
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
