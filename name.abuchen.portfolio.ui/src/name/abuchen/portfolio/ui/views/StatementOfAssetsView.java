package name.abuchen.portfolio.ui.views;

import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;

public class StatementOfAssetsView extends AbstractFinanceView
{
    private StatementOfAssetsViewer assetViewer;
    private PropertyChangeListener currencyChangeListener;
    private ClientFilterDropDown clientFilter;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getDefaultTitle()
    {
        return assetViewer == null ? Messages.LabelStatementOfAssets : Messages.LabelStatementOfAssets + //
                        " (" + assetViewer.getColumnHelper().getConfigurationName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void notifyModelUpdated()
    {
        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        Client filteredClient = clientFilter.getSelectedFilte().filter(getClient());
        ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, converter, LocalDate.now());

        assetViewer.setInput(snapshot);
        updateTitle(getDefaultTitle());
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        AbstractDropDown dropdown = new AbstractDropDown(toolBar, getClient().getBaseCurrency())
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                List<CurrencyUnit> available = CurrencyUnit.getAvailableCurrencyUnits();
                Collections.sort(available);
                for (final CurrencyUnit unit : available)
                {
                    Action action = new SimpleAction(unit.getLabel(), a -> {
                        setLabel(unit.getCurrencyCode());
                        getClient().setBaseCurrency(unit.getCurrencyCode());
                    });
                    action.setChecked(getClient().getBaseCurrency().equals(unit.getCurrencyCode()));
                    manager.add(action);
                }
            }
        };
        currencyChangeListener = e -> dropdown.setLabel(e.getNewValue().toString());
        getClient().addPropertyChangeListener("baseCurrency", currencyChangeListener); //$NON-NLS-1$

        this.clientFilter = new ClientFilterDropDown(toolBar, getClient(), filter -> notifyModelUpdated());

        Action export = new SimpleAction(null, action -> new TableViewerCSVExporter(assetViewer.getTableViewer())
                        .export(Messages.LabelStatementOfAssets + ".csv")); //$NON-NLS-1$
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);
        new ActionContributionItem(export).fill(toolBar, -1);

        Action save = new SimpleAction(null, a -> assetViewer.showSaveMenu(getActiveShell()));
        save.setImageDescriptor(Images.SAVE.descriptor());
        save.setToolTipText(Messages.MenuSaveColumns);
        new ActionContributionItem(save).fill(toolBar, -1);

        Action config = new SimpleAction(null, a -> assetViewer.showConfigMenu(toolBar.getShell()));
        config.setImageDescriptor(Images.CONFIG.descriptor());
        config.setToolTipText(Messages.MenuShowHideColumns);
        new ActionContributionItem(config).fill(toolBar, -1);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        assetViewer = make(StatementOfAssetsViewer.class);
        Control control = assetViewer.createControl(parent);

        updateTitle(getDefaultTitle());
        assetViewer.getColumnHelper().addListener(() -> updateTitle(getDefaultTitle()));

        hookContextMenu(assetViewer.getTableViewer().getControl(),
                        manager -> assetViewer.hookMenuListener(manager, StatementOfAssetsView.this));
        notifyModelUpdated();

        return control;
    }

    @Override
    public void dispose()
    {
        if (currencyChangeListener != null)
            getClient().removePropertyChangeListener("baseCurrency", currencyChangeListener); //$NON-NLS-1$
    }
}
