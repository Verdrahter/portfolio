package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.PieChart;
import name.abuchen.portfolio.ui.util.ViewDropdownMenu;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

public class StatementOfAssetsPieChartView extends AbstractFinanceView
{
    private static final String IDENTIFIER = StatementOfAssetsPieChartView.class.getName() + "-VIEW"; //$NON-NLS-1$

    private ViewDropdownMenu dropdown;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsClasses;
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        super.addButtons(toolBar);
        dropdown = new ViewDropdownMenu(toolBar);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new StackLayout());

        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), Dates.today());
        List<AssetCategory> categories = snapshot.groupByCategory();

        createPieChart(container, categories);
        createTreeMap(container, snapshot, categories);

        dropdown.select(getClientEditor().getPreferenceStore().getInt(IDENTIFIER));

        return container;
    }

    private void createPieChart(Composite container, List<AssetCategory> categories)
    {
        PieChart pieChart = new PieChart(container, SWT.NONE);

        List<PieChart.Slice> slices = new ArrayList<PieChart.Slice>();

        for (AssetClass a : AssetClass.values())
            slices.add(new PieChart.Slice(categories.get(a.ordinal()).getValuation(), a.name(),
                            Colors.valueOf(a.name())));

        pieChart.setSlices(slices);
        pieChart.redraw();

        dropdown.add(Messages.LabelViewPieChart, PortfolioPlugin.IMG_VIEW_PIECHART, pieChart);
    }

    private void createTreeMap(Composite parent, ClientSnapshot snapshot, List<AssetCategory> categories)
    {
        TreeMapItem rootItem = new TreeMapItem();
        for (AssetCategory category : categories)
        {
            if (category.getAssetClass() == null)
                continue;

            TreeMapItem categoryItem = new TreeMapItem(rootItem, category);
            rootItem.getChildren().add(categoryItem);

            for (AssetPosition position : category.getPositions())
            {
                TreeMapItem positionItem = new TreeMapItem(categoryItem, position);
                categoryItem.getChildren().add(positionItem);
            }
        }

        rootItem.pruneEmpty();
        rootItem.calculatePercentages(snapshot.getAssets());
        rootItem.sortBySize();

        List<Colors> colors = new ArrayList<Colors>();
        for (TreeMapItem child : rootItem.getChildren())
            colors.add(Colors.valueOf(child.getAssetCategory().getAssetClass().name()));

        ColorWheel colorWheel = new ColorWheel(parent, colors);

        TreeMapViewer viewer = new TreeMapViewer(parent, SWT.NONE);
        viewer.setInput(rootItem, colorWheel);
        dropdown.add(Messages.LabelViewTreeMap, PortfolioPlugin.IMG_VIEW_TREEMAP, viewer.getControl());
    }

    @Override
    public void dispose()
    {
        getClientEditor().getPreferenceStore().setValue(IDENTIFIER, dropdown.getSelectedIndex());
        super.dispose();
    }
}
