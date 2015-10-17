package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class FlatexPDFExctractor extends AbstractPDFExtractor
{

    public FlatexPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBuySellTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Sammelabrechnung (Wertpapierkauf/-verkauf)");
        this.addDocumentTyp(type);

        Block block = new Block("Nr.(\\d*)/(\\d*)  Kauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Kauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "date")
                        .match("^davon ausgef.: (?<shares>\\d+,\\d*) St. *Schlusstag *:  (?<date>\\d+.\\d+.\\d{4}+), \\d+:\\d+ Uhr")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));

                        })

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));

        block = new Block("Nr.(\\d*)/(\\d*)  Verkauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Verkauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "date")
                        .match("^davon ausgef.: (?<shares>\\d+,\\d*) St. *Schlusstag *:  (?<date>\\d+.\\d+.\\d{4}+), \\d+:\\d+ Uhr")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));

                        })

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("tax", "currency").optional() //
                        .match(".* \\*\\*Einbeh. Steuer *: *(?<tax>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendengutschrift.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                        //
                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*) * (?<name>[^(]*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares") //
                        .match("^St. *: *(?<shares>[\\.\\d]+(,\\d*)?)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("date") //
                        .match("Valuta * : *(?<date>\\d+.\\d+.\\d{4}+) *")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "flatex"; //$NON-NLS-1$
    }
}
