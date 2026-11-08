/**
 * Verificacion de base de datos (FASE A).
 * Uso: npx tsx scripts/verify-database.ts
 */
import { getSupabaseService } from "../src/integrations/supabase";

async function main() {
  const supabase = getSupabaseService();

  const { data: stocks, error: errStocks } = await supabase
    .from("stocks")
    .select("symbol, name, is_popular")
    .order("symbol");

  if (errStocks) {
    console.error("stocks:", errStocks.message);
  } else {
    console.log(
      `stocks OK -> ${stocks?.length} empresas: ${stocks?.map((s) => s.symbol).join(", ")}`
    );
  }

  const { data: packages, error: errPkgs } = await supabase
    .from("packages")
    .select("code, name, price_mxn, credits, active")
    .order("price_mxn");

  if (errPkgs) {
    console.error("packages:", errPkgs.message);
  } else {
    console.log(
      `packages OK -> ${packages?.length} paquetes: ${packages
        ?.map((p) => `${p.name} $${p.price_mxn} MXN = ${p.credits} creditos`)
        .join(" | ")}`
    );
  }

  // RLS leve: sin JWT de usuario, el cliente anon no debe ver saldos de nadie
  const { getSupabasePublishable } = await import("../src/integrations/supabase");
  const { data: walletsAnon, error: errWallets } = await getSupabasePublishable()
    .from("virtual_wallets")
    .select("user_id, balance");

  if (errWallets) {
    console.log(`RLS OK -> cliente publico NO ve saldos (${errWallets.message})`);
  } else if (!walletsAnon || walletsAnon.length === 0) {
    console.log("RLS OK -> cliente publico no ve ninguna fila de saldos");
  } else {
    console.warn(`ATENCION: cliente publico ve ${walletsAnon.length} saldos!`);
  }
}

main().catch((e) => {
  console.error("Error de verificacion:", e instanceof Error ? e.message : e);
  process.exit(1);
});
