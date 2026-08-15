# xiaolehuskhomesmenu

适用于 Folia 26.1.2 的 HuskHomes 箱子菜单插件：

- `/tpamenu` 使用玩家头颅选择在线玩家并发送 TPA 请求。
- `/warpmenu` 读取 HuskHomes 官方 Warp 数据，点击图标传送。
- 两个菜单均支持自动分页、刷新、关闭和配置重载。

## 运行需求

- Java 25
- Folia 26.1.2
- HuskHomes 4.10
- 可选安装 SkinsRestorer，用于离线服玩家头颅皮肤

## 构建

```bash
mvn package
```

生成的插件文件：

```text
target/xiaolehuskhomesmenu-1.1.0.jar
```

## 使用

- `/tpamenu` 打开传送请求菜单。
- `/xiaolehuskhomesmenu`、`/xiaoletpamenu`、`/tpmenu`、`/tpagui` 是别名。
- `/warpmenu` 打开地标传送菜单。
- `/warpui`、`/warpgui` 是地标菜单别名。
- `/tpamenu reload` 或 `/warpmenu reload` 重载全部 `config.yml` 配置。

权限：

- `tpamenu.open`，默认所有玩家拥有
- `tpamenu.warp.open`，默认所有玩家拥有
- `tpamenu.reload`，默认 OP 拥有

## 说明

插件通过配置里的命令发送传送请求：

```yaml
menu:
  tpa-command: "tpa {player}"
```

这样冷却、请求处理、权限和安全检查都继续交给 HuskHomes 负责。

Warp 列表来自 HuskHomes 4.10 官方 API，点击后默认执行：

```yaml
warp-menu:
  warp-command: "huskhomes:warp {warp}"
```

传送冷却、预热、权限、经济扣费和跨服处理仍由 HuskHomes 完成。本插件不会创建、删除或修改 Warp。

默认图标与描述可在 `warp-menu.items.warp` 修改，也可按 Warp 名称覆盖：

```yaml
warp-menu:
  warps:
    spawn:
      material: GRASS_BLOCK
      name: "&a服务器主城"
      lore:
        - "&7返回服务器主城"
        - ""
        - "&e点击传送"
```

每页默认显示 45 个 Warp。超过 45 个时显示下一页按钮；翻页后 Warp 数量变化时，页码会自动修正到有效范围。
