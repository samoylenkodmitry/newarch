# Брейншторм новой архитектуры (newarch)

См. юнит-тесты.

Принципы новой архитектуры:
* она должна быть удобной для разработчика
    * понятное и простое расположение файлов
    * минимум лишнего бойлерплейта, никаких лишних действий, каждая переменная/метод должны быть обоснованы
    * названия, подсказывающие разработчику, что от него ждут
    * автоподсказка по ctrl+space не должна высвечивать ничего лишнего
    * разработчику просто перемещаться по всем классам экрана, не требуется ничего искать
* строгое разделение сущностей. Если это экран, то в нем только рендеринг. Если презентер, то только подготовка состояний экрана. Бизнес-логика и все ее хвосты (названия переменных, объекты, алгоритмы) не должны попадать в слой рендеринга.
* архитектура должна скрывать от разработчика необходимость следить за потоками. Минимум главного потока для отрисовки. Остальные места - в бэкграунде.
* постараться избавить разработчика от жизненного цикла экрана, автоматизировать очистку/перерисовку при поворотах, выходах

# Как сделать pull-реквест
https://www.dataschool.io/how-to-contribute-on-github/
