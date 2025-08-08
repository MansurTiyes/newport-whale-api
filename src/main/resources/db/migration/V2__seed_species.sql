-- V2__seed_species.sql
-- Seeds canonical species metadata + alias arrays for scraper resolution.
-- Idempotent: safe to run multiple times.

BEGIN;

-- Whales
INSERT INTO species (id, "group", common_name, binomial_name, aliases) VALUES
                                                                           ('gray-whale',   'whale', 'Gray whale',        'Eschrichtius robustus',
                                                                            ARRAY['gray','gray whale','gray whales','grey','grey whales']),
                                                                           ('blue-whale',   'whale', 'Blue whale',        'Balaenoptera musculus',
                                                                            ARRAY['blue','blue whale','blue whales']),
                                                                           ('fin-whale',    'whale', 'Fin whale',         'Balaenoptera physalus',
                                                                            ARRAY['fin whale','fin whales','fin']),
                                                                           ('humpback-whale','whale','Humpback whale',    'Megaptera novaeangliae',
                                                                            ARRAY['humpback','humpback whale','humpback whales','humpbacks']),
                                                                           ('minke-whale',  'whale', 'Minke whale',       'Balaenoptera acutorostrata',
                                                                            ARRAY['minke','minke whale','minkes','minke whales']),
                                                                           ('orca',         'whale', 'Orca',              'Orcinus orca',
                                                                            ARRAY['orca','killer whale','killer whales']),
                                                                           ('brydes-whale', 'whale', 'Bryde''s whale',    'Balaenoptera brydei',
                                                                            ARRAY['bryde','bryde''s whale','brydes whale','bryde''s whales'])
ON CONFLICT (id) DO NOTHING;

-- Dolphins
INSERT INTO species (id, "group", common_name, binomial_name, aliases) VALUES
                                                                           ('common-dolphin', 'dolphin', 'Common dolphin', 'Delphinus delphis',
                                                                            ARRAY['common dolphin','common dolphins','common']),
                                                                           ('bottlenose-dolphin', 'dolphin', 'Bottlenose dolphin', 'Tursiops truncatus',
                                                                            ARRAY['bottlenose dolphin','bottlenose dolphins','bottlenose']),
                                                                           ('pacific-white-sided-dolphin', 'dolphin', 'Pacific white-sided dolphin', 'Lagenorhynchus obliquidens',
                                                                            ARRAY['pws dolphin','pacific white-sided dolphin','pacific white sided dolphin','white-sided dolphin','white sided dolphin']),
                                                                           ('rissos-dolphin', 'dolphin', 'Risso''s dolphin', 'Grampus griseus',
                                                                            ARRAY['risso','risso''s dolphin','risso''s dolphins','rissos dolphin','risso dolphin'])
ON CONFLICT (id) DO NOTHING;

-- Sharks
INSERT INTO species (id, "group", common_name, binomial_name, aliases) VALUES
                                                                           ('mako-shark', 'shark', 'Mako shark', 'Isurus oxyrinchus',
                                                                            ARRAY['mako shark','mako','mako sharks']),
                                                                           ('thresher-shark', 'shark', 'Thresher shark', 'Alopias vulpinus',
                                                                            ARRAY['thresher shark','thresher','thresher sharks']),
                                                                           ('hammerhead-shark', 'shark', 'Hammerhead shark', 'Sphyrna zygaena',
                                                                            ARRAY['hammerhead shark','hammerhead','hammerhead sharks']),
                                                                           ('white-shark', 'shark', 'White shark', 'Carcharodon carcharias',
                                                                            ARRAY['white shark','white sharks','great white','great white shark'])
ON CONFLICT (id) DO NOTHING;

-- Fish
INSERT INTO species (id, "group", common_name, binomial_name, aliases) VALUES
    ('sunfish', 'fish', 'Ocean sunfish', 'Mola mola',
     ARRAY['sunfish','ocean sunfish','mola mola'])
ON CONFLICT (id) DO NOTHING;

-- Other
INSERT INTO species (id, "group", common_name, binomial_name, aliases) VALUES
    ('false-killer-whale', 'other', 'False killer whale', 'Pseudorca crassidens',
     ARRAY['false killer whale','false-killer whale','false killer'])
ON CONFLICT (id) DO NOTHING;

COMMIT;
