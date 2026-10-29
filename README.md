# SEER — Search & Explore Every Recipe

*English · [Русский](#русский)*

SEER is an item and recipe browser for **Minecraft 1.20.1 (Forge)**, in the spirit of EMI and REI.
Its interface is built on [OpenUI MC](https://github.com/UpperMoon0/OpenUI-MC).

> Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

## Features

- **Item index** of every item and fluid, including creative-tab variants (potions, enchanted books, tipped arrows…), in creative order.
- **Search** with a small query language:

  | Query | Meaning |
  |---|---|
  | `sword axe` | space = AND |
  | `sword\|axe` | `\|` = OR |
  | `-wood` | exclude |
  | `@create` | by mod id or mod name |
  | `$fire` | by tooltip text |
  | `#planks` | by tag |
  | `/^iron_.*/` | regular expression |

- **Two panels** next to any container screen:
  - right: all items;
  - left: your **favorites**, or what you can **craft right now** from your inventory (button with the crafting table, or the vanilla recipe book button).
- **Recipes and uses screen** with category tabs, paged recipe cards, and a mouse wheel to flip pages.
  The selected tab is remembered while you browse.
- **Workstation view:** click the selected tab (or right-click any tab) to see *everything* a crafting table, furnace, stonecutter… can make.
- **Recipe types:** crafting (shaped and shapeless, always shown as a 3×3 grid), smelting, blasting, smoking, campfire cooking, stonecutting, smithing.
- **One-click recipe transfer** (the `+` button on a card) for the crafting table, the 2×2 inventory grid, furnaces, blast furnaces and smokers. `Shift` places as many as possible. Needs SEER on the server too.
- **Favorites** (`A`), saved to `config/seer-favorites.json`.
- **Crafting tree** button (placeholder, coming soon).
- **Vanilla recipe book is disabled** (via a mixin); SEER replaces it.
- **Effect icons** are redrawn in a row above the inventory so they never collide with the side panels.
- **Translucent frames** with an opacity slider in the settings (gear button, bottom left).
- **Adaptive layout:** panels follow the container GUI, move around widgets that stick out, and hide when there is no room.
- **Plugin API** for other mods (see below).

## Controls

| Action | Input |
|---|---|
| Recipes of the hovered item | Left click on an item in a panel, or `R` |
| Uses of the hovered item | Right click on an item in a panel, or `U` |
| Add / remove favorite | `A` over an item, or the star on a recipe card |
| Back (recipe screen) | `Backspace` or the mouse side button; with no history it returns to the item list |
| Flip pages | Mouse wheel or the arrow buttons |
| Show / hide panels | `Ctrl+O` or the gear → *Show item panel* |
| Clear the search | Right click the search field |

`R`, `U` and `A` also work on items in inventory slots. Clicks on vanilla slots keep their normal behavior.

## Requirements

- Minecraft **1.20.1**, Forge **47.x**, Java **17**
- [OpenUI MC](https://www.curseforge.com/minecraft/mc-mods/openui-mc) (required on the client)
- Optional: install SEER on the server to enable recipe transfer

## Configuration

`config/seer-client.toml`

```toml
[overlay]
    visible = true   # show the item panels
    opacity = 85     # 30-100, lower = more transparent (also adjustable in the settings screen)
```

Debug commands: `/seer <query>` (search from chat), `/seerinfo <item id>` (recipe and use counts).

## Building

```bash
./gradlew build        # jar in build/libs
./gradlew runClient    # development client
```

Setup notes:

- Uses ForgeGradle 6 with official mappings and **MixinGradle** (`seer.mixins.json`).
- OpenUI MC is pulled through CurseMaven: `implementation fg.deobf('curse.maven:openui-mc-1661978:9000408')`.

## Plugin API

Other mods can add categories, workstations, recipes and extra index entries.
Annotate a class and implement `SeerPlugin`; it is discovered automatically.

```java
@SeerEntrypoint
public final class MyPlugin implements SeerPlugin {
    @Override
    public void register(SeerRegistry registry) {
        SeerCategory category = new SeerCategory(
                new ResourceLocation("mymod", "grinding"),
                Component.literal("Grinding"),
                SeerStack.of(new ItemStack(MyItems.GRINDER.get())));
        registry.addCategory(category);
        registry.addRecipe(new SeerRecipe(
                new ResourceLocation("mymod", "grind_stone"), category,
                List.of(SeerIngredient.of(SeerStack.of(new ItemStack(Items.STONE)))),
                List.of(SeerStack.of(new ItemStack(Items.GRAVEL))),
                0, 0, 0, 0f));
    }
}
```

Screens with widgets SEER cannot detect can reserve space with `SeerExclusionZones.register(MyScreen.class, screen -> List.of(new Rect2i(...)))`.

## Project layout

| Package | Contents |
|---|---|
| `dev.seer` | mod entry point, config |
| `dev.seer.core` | stacks, index, search, recipe model, vanilla recipe loader, plugin loading |
| `dev.seer.api` | public plugin API |
| `dev.seer.client` | OpenUI panels, recipe screen, settings, favorites, effects |
| `dev.seer.network` | recipe transfer packet |
| `dev.seer.mixin` | vanilla recipe book mixin |

## Roadmap

- Crafting tree (the button is already in place)
- Item categories in the panel
- Brewing, anvil and enchanting recipes
- Recipes added by other mods without a plugin
- Status effect HUD

## Credits and license

Inspired by [EMI](https://github.com/emilyploszaj/emi) and [Roughly Enough Items](https://github.com/shedaniel/RoughlyEnoughItems) (both MIT).
SEER is written from scratch and contains no code from those projects.
The interface uses [OpenUI MC](https://github.com/UpperMoon0/OpenUI-MC) (MIT), which is not bundled.

SEER is released under the [MIT License](LICENSE).

---

# Русский

SEER — просмотрщик предметов и рецептов для **Minecraft 1.20.1 (Forge)** в духе EMI и REI.
Интерфейс построен на [OpenUI MC](https://github.com/UpperMoon0/OpenUI-MC).

> Не является официальным продуктом Minecraft, не одобрен и не связан с Mojang или Microsoft.

## Возможности

- **Индекс** всех предметов и жидкостей, включая варианты из творческих вкладок (зелья, зачарованные книги, тайные стрелы…), в порядке креатива.
- **Поиск:** пробел = И, `|` = ИЛИ, `-слово` = исключить, `@мод`, `$текст тултипа`, `#тег`, `/регулярка/`.
- **Две панели** у любого контейнерного экрана:
  - справа все предметы;
  - слева **избранное** или то, что можно **скрафтить прямо сейчас** из инвентаря (кнопка с верстаком или кнопка ванильной книги рецептов).
- **Экран рецептов и применений:** вкладки категорий, карточки рецептов по страницам, страницы листаются колесом мыши. Выбранная вкладка запоминается.
- **Режим станка:** клик по выбранной вкладке (или ПКМ по любой) показывает всё, что делается на верстаке, в печи, камнерезе и так далее.
- **Типы рецептов:** крафт (по форме и бесформенный, всегда сеткой 3×3), плавка, плавильня, коптильня, костёр, камнерез, кузнечный стол.
- **Перенос рецепта одной кнопкой** (`+` на карточке) для верстака, сетки 2×2 в инвентаре, печей. `Shift` кладёт максимум. Нужен SEER и на сервере.
- **Избранное** (`A`) сохраняется в `config/seer-favorites.json`.
- **Дерево крафта:** кнопка-заглушка (скоро).
- **Ванильная книга рецептов отключена** (миксином), вместо неё SEER.
- **Иконки эффектов** перерисованы рядом над инвентарём и не мешают боковым панелям.
- **Прозрачные рамки** со слайдером в настройках (шестерёнка слева снизу).
- **Адаптивная раскладка:** панели подстраиваются под GUI, обходят выступающие виджеты и скрываются, если нет места.
- **API для плагинов** других модов.

## Управление

| Действие | Ввод |
|---|---|
| Рецепты предмета | ЛКМ по предмету в панели или `R` |
| Применения предмета | ПКМ по предмету в панели или `U` |
| Избранное | `A` над предметом или звезда на карточке |
| Назад (экран рецептов) | `Backspace` или боковая кнопка мыши; без истории возвращает к списку предметов |
| Листать страницы | колесо мыши или стрелки |
| Показать / скрыть панели | `Ctrl+O` или шестерёнка → *Show item panel* |
| Очистить поиск | ПКМ по полю поиска |

## Требования

- Minecraft **1.20.1**, Forge **47.x**, Java **17**
- [OpenUI MC](https://www.curseforge.com/minecraft/mc-mods/openui-mc) на клиенте (обязательно)
- Для переноса рецептов SEER нужен и на сервере

## Настройка и сборка

Конфиг `config/seer-client.toml`: `overlay.visible` (показывать панели) и `overlay.opacity` (30–100, меньше значит прозрачнее, то же делает слайдер в настройках).

```bash
./gradlew build        # jar в build/libs
./gradlew runClient    # клиент для разработки
```

Сборка: ForgeGradle 6, официальные маппинги, MixinGradle (`seer.mixins.json`); OpenUI MC подключается через CurseMaven.

## Лицензия

MIT, см. [LICENSE](LICENSE). Вдохновлено EMI и REI (оба MIT), код этих проектов не использовался.
