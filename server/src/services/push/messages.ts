// Textes des notifications (FR/EN), portés à l'identique depuis
// supabase/functions/notify/index.ts pour garantir la parité de contenu.

export type PushEvent =
  | "threshold"
  | "complete"
  | "distribute_prompt"
  | "distributed"
  | "selection_reminder"
  | "final_reminder"
  | "selection_extended"
  | "auto_extended"
  | "deleted";

export interface PushMessage {
  title: string;
  body: string;
}

export function messageFor(
  event: string,
  value: number | null,
  chainName: string,
  locale: string,
): PushMessage {
  const en = (locale || "fr").toLowerCase().startsWith("en");
  if (event === "threshold") {
    return {
      title: en ? "Tehilim chain" : "Chaîne de Tehilim",
      body: en ? `“${chainName}” is ${value}% complete` : `« ${chainName} » est complétée à ${value} %`,
    };
  }
  if (event === "distributed") {
    return {
      title: en ? "Chain distributed 🙏" : "Chaîne distribuée 🙏",
      body: en
        ? `“${chainName}” has been distributed — happy reading`
        : `« ${chainName} » a été distribuée — bonne lecture`,
    };
  }
  if (event === "complete") {
    return {
      title: en ? "Chain complete 🎉" : "Chaîne complétée 🎉",
      body: en
        ? `“${chainName}” — all 150 Tehilim are assigned!`
        : `« ${chainName} » — les 150 Tehilim sont attribués !`,
    };
  }
  if (event === "distribute_prompt") {
    return {
      title: en ? "Your turn to distribute 🙏" : "À toi de distribuer 🙏",
      body: en
        ? `“${chainName}” is complete — distribute it to start the reading`
        : `« ${chainName} » est complète — distribue-la pour lancer la lecture`,
    };
  }
  if (event === "selection_reminder") {
    return {
      title: en ? "Selection closing soon" : "Sélection bientôt close",
      body: en
        ? `“${chainName}”: ${value} Tehilim left to pick`
        : `« ${chainName} » : il reste ${value} Tehilim à prendre`,
    };
  }
  if (event === "final_reminder") {
    return {
      title: en ? "Last chance ⏳" : "Dernière chance ⏳",
      body: en
        ? `“${chainName}” closes very soon — ${value} Tehilim still free`
        : `« ${chainName} » ferme très bientôt — ${value} Tehilim encore libres`,
    };
  }
  if (event === "selection_extended") {
    return {
      title: en ? "Selection extended ⏰" : "Sélection prolongée ⏰",
      body: en
        ? `“${chainName}”: more time to pick your Tehilim`
        : `« ${chainName} » : plus de temps pour choisir vos Tehilim`,
    };
  }
  if (event === "auto_extended") {
    return {
      title: en ? "Selection extended +3h ⏰" : "Sélection prolongée de 3 h ⏰",
      body: en
        ? `“${chainName}”: ${value} Tehilim still free — 3 extra hours to pick`
        : `« ${chainName} » : ${value} Tehilim encore libres — 3 h de plus pour choisir`,
    };
  }
  return {
    title: en ? "Chain deleted" : "Chaîne supprimée",
    body: en ? `“${chainName}” was deleted by its creator` : `« ${chainName} » a été supprimée par son créateur`,
  };
}
